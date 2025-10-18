package de.schosin.decs.core.data;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import de.schosin.decs.api.World;
import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.api.entities.Transition;
import de.schosin.decs.api.entities.Transmutation;
import de.schosin.decs.api.exceptions.EntityDeletedException;
import de.schosin.decs.api.exceptions.EntityModifiedException;
import de.schosin.decs.api.internal.EntityArchetypeData;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.api.utils.collections.CollectionUtils;
import de.schosin.decs.api.utils.collections.IntBag;
import de.schosin.decs.api.utils.collections.IntIntMap;
import de.schosin.decs.api.utils.pool.Pool;
import de.schosin.decs.api.utils.pool.Pooled;
import de.schosin.decs.values.Components;
import de.schosin.decs.values.Types;

public final class EntityArchetypeImpl implements EntityArchetype {

    private static final int ENTITY_BAG_SIZE = 1024;

    private static final int DELETED = -2;

    private static final int DIRTY = 1;
    private static final int DIRTY_MOVE = 2;

    private final int id;
    private final EntityIndex entityIndex;

    private final List<Class<?>> components;
    private final EntityArchetypeData data;

    private final IntBag entities = new IntBag(ENTITY_BAG_SIZE);
    private final IntIntMap indices = new IntIntMap(ENTITY_BAG_SIZE);

    private final Pool<EntityRefImpl> referencePool = Pool.unbounded(ENTITY_BAG_SIZE, EntityRefImpl.class, EntityRefImpl::new);
    private final Bag<Bag<EntityRefImpl>> references = new Bag<>(Bag.class, ENTITY_BAG_SIZE);

    private final int lockSize;
    private final Lock[] locks;
    private final Lock dataLock;

    private final Bag<Callback> insertedCallbacks = new Bag<>(Callback.class, 8);
    private final Bag<Callback> removedCallbacks = new Bag<>(Callback.class, 8);
    private final Bag<EntityArchetypeListener> inserted = new Bag<>(EntityArchetypeListener.class, 4);
    private final Bag<EntityArchetypeListener> removed = new Bag<>(EntityArchetypeListener.class, 4);

    private final Pool<TransitionImpl> transitions = Pool.unbounded(8, TransitionImpl.class, TransitionImpl::new);
    private final Bag<EntityArchetypeImpl> targetArchetypes = new Bag<>(EntityArchetypeImpl.class, 8); // index: Transmutation#id()
    private final Bag<ArchetypeMover> movers = new Bag<>(ArchetypeMover.class, 8); // index: EntityArchetype#id()

    private final AtomicInteger created = new AtomicInteger(-1); // index of first entity created in this archetype
    private final IntBag createdEntities = new IntBag(256); // indices of entities created in this archetype after the first moved entity

    private final AtomicInteger moved = new AtomicInteger(-1); // index of first entity moved from this to another archetype
    private final Bag<MovedEntities> movedEntities = new Bag<>(MovedEntities.class, 8); // indices of entities moved from this to another archetype
    private final Bag<MovedEntities> movedEntitiesLookup = new Bag<>(MovedEntities.class, 16); // lookup indexed by #id

    private final IntBag deletedEntities = new IntBag(256); // indices of deleted entities
    private final IntIntMap changedEntitiesLookup = new IntIntMap(1024); // lookup from index to #DELETED or id of target archetype

    private int dirty = 0; // #DIRTY or #DIRTY_MOVE

    /**
     * For debugging purposes during development, will be removed / changed to a opt-in later.
     */
    private void validateState() {
        int expected = this.entities.size();

        // TODO reimplement validation
        this.data.validateState(expected);
        /*
        Bag<Object>[] data = this.data.getData();
        for (int c = 0, cs = data.length; c < cs; c++) {
            Bag<Object> components = data[c];
            if (components != null) {
                for (int i = 0; i < expected; i++) {
                    if (components.get(i) == null) {
                        throw new IllegalStateException(String.format("Index %d has a null component %s", i, this.components.get(c).getSimpleName()));
                    }
                }
            }
        }
        */

        for (int d = 0, ds = this.deletedEntities.size(); d < ds; d++) {
            int index = deletedEntities.get(d);
            if (index >= expected) {
                throw new IllegalStateException(String.format("Index %d of deleted entity higher than entities size %d", index, expected));
            }
        }

        for (int e = 0, es = this.references.size(); e < es; e++) {
            Bag<EntityRefImpl> refs = this.references.get(e);
            for (int r = 0, rs = refs.size(); r < rs; r++) {
                EntityRefImpl ref = refs.get(r);
                if (ref.index >= expected) {
                    throw new IllegalStateException(String.format("Reference to index %d higher than entities size %d", ref.index, expected));
                }
            }
        }
    }

    public EntityArchetypeImpl(int id, World world, EntityIndex entityIndex, ComponentIndex componentIndex, List<Class<?>> components, int lockSize, Supplier<Lock> lock) {
        this.id = id;
        this.entityIndex = entityIndex;

        this.components = CollectionUtils.listOf(components);
        this.data = (EntityArchetypeData) Types.createArchetypeEntityData(ENTITY_BAG_SIZE, components);

        this.lockSize = lockSize;
        this.locks = new Lock[lockSize];
        for (int i = 0; i < lockSize; i++) {
            this.locks[i] = lock.get();
        }

        this.dataLock = lock.get();
    }

    public final void offerArchetype(EntityArchetypeImpl archetype) {
        // Initialize archetype helper types to avoid branches when using lazy initialization
        this.movers.set(archetype.id, new ArchetypeMover(this, archetype));

        // Initialize moved entities and callbacks
        MovedEntities entities = new MovedEntities(archetype);
        this.movedEntitiesLookup.set(archetype.id, entities);

        for (Callback callback : insertedCallbacks) {
            if (callback.composition().matches(archetype)) {
                entities.inserted.add(callback.factory().apply(archetype));
            }
        }

        for (Callback callback : removedCallbacks) {
            if (!callback.composition().matches(archetype)) {
                entities.removed.add(callback.factory().apply(archetype));
            }
        }
    }

    public final void offerInserted(Callback callback) {
        Composition composition = callback.composition();
        Function<EntityArchetype, EntityArchetypeListener> factory = callback.factory();

        boolean matches = composition.matches(this);
        if (!matches) {
            this.insertedCallbacks.add(callback);
        }

        if (matches) {
            this.inserted.add(factory.apply(this));
        }

        for (MovedEntities entities : movedEntitiesLookup) {
            if (entities != null && !matches && composition.matches(entities.archetype)) {
                entities.inserted.add(factory.apply(entities.archetype));
            }
        }
    }

    public final void offerRemoved(Callback callback) {
        Composition composition = callback.composition();
        Function<EntityArchetype, EntityArchetypeListener> factory = callback.factory();

        boolean matches = composition.matches(this);
        if (matches) {
            this.removedCallbacks.add(callback);
        }

        if (matches) {
            this.removed.add(factory.apply(this));
        }

        for (MovedEntities entities : movedEntitiesLookup) {
            if (entities != null && matches && !composition.matches(entities.archetype)) {
                entities.removed.add(factory.apply(this));
            }
        }
    }

    @Override
    public final int id() {
        return id;
    }

    @Override
    public final List<Class<?>> getComponents() {
        return this.components;
    }

    @Override
    public final IntBag getEntities() {
        return entities;
    }

    @Override
    public final int getAlive() {
        // TODO consider explicit field instead

        int created = this.created.get();
        int moved = this.moved.get();

        if (created == -1) {
            return moved == -1 ? entities.size() : moved;
        }

        if (moved == -1) {
            return created == -1 ? entities.size() : created;
        }

        return created < moved ? created : moved;
    }

    @Override
    public final <T> Bag<T> getData(Class<T> type) {
        return this.data.getData(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends EntityArchetypeData> T getData() {
        return (T) this.data;
    }

    @Override
    public final int createEntities(int count) {
        int index;
        synchronized (this.entities) {
            index = this.entities.size();

            // Track first created entity
            boolean moved;
            if (!(moved = this.moved.get() != -1)) {
                this.created.compareAndSet(-1, index);
            }

            // Ensure capacity for data
            int s = index + count;
            if (s >= this.entities.getCapacity()) {
                this.dataLock.lock();

                try {
                    if (s >= this.entities.getCapacity()) {
                        this.entities.ensureCapacity(s);
                        this.references.ensureCapacity(s);
                        this.data.ensureCapacity(s);
                    }
                } finally {
                    this.dataLock.unlock();
                }
            }

            // Retrieve entity ids, initialize components
            for (int i = index; i < s; i++) {
                this.entities.setUnsafe(i, this.entityIndex.getEntityId());
                this.data.initialize(i);

                if (moved) {
                    this.createdEntities.add(i);
                }
            }
        }

        markDirty(DIRTY);
        return index;
    }

    @Override
    public final int getEntityId(int index) {
        return entities.get(index);
    }

    @Override
    public final void deleteEntity(int index) {
        validateState();

        // Throw error if entity being created
        int created = this.created.get();
        if (created > -1 && index >= created) {
            throw new UnsupportedOperationException("Cannot delete entities being created.");
        }

        // Handle already deleted entities
        int targetArchetypeId = this.changedEntitiesLookup.getUnsafe(index);
        if (targetArchetypeId <= DELETED) {
            return;
        }

        markDirty(DIRTY);

        // Handle entity marked for move to another archetype
        if (targetArchetypeId > -1) {
            // TODO can this be changed to just throw? parallel systems writing the same entity should not be supported, should it?

            // Delete in target archetype
            ArchetypeMover mover = this.movers.get(targetArchetypeId);
            mover.deleteAddedEntity(this.entities.get(index));
            validateState();

            // Remove from moved entities
            this.movedEntitiesLookup.get(targetArchetypeId).remove(index);
        }

        // Determine index to insert to keep deletedEntities sorted
        int[] data = deletedEntities.getData();
        for (int i = 0, s = deletedEntities.size(); i < s; i++) {
            int value = data[i];
            if (index < value) {
                deletedEntities.insert(i, index);
                changedEntitiesLookup.set(index, DELETED - i);
                validateState();

                return;
            }
        }

        // First deleted entity inserted at index 0
        deletedEntities.insert(0, index);
        changedEntitiesLookup.set(index, DELETED);
        validateState();
    }

    @Override
    public final Transition moveEntity(int index, Transmutation transmutation) {
        // TODO see if lock scope can be made smaller 
        // TODO createEntities, moveEntity, deleteEntity must not be used when processing archetype

        // Throw error if entity being created
        int created = this.created.get();
        if (created > -1 && index >= created) {
            throw new UnsupportedOperationException("Cannot change entities being created.");
        }

        Lock lock = getLock(index);
        lock.lock();

        try {
            return doMoveEntity(index, transmutation);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Moves an entity to another archetype based on the transmutation.
     */
    private Transition doMoveEntity(int index, Transmutation transmutation) {
        // Validate not deleted or already moved
        int pendingArchetypeId = this.changedEntitiesLookup.getUnsafe(index);
        if (pendingArchetypeId != -1) {
            int entityId = this.entities.get(index);

            throw pendingArchetypeId <= DELETED
                    ? new EntityDeletedException(entityId, "Deleted entities cannot be modified.")
                    : new EntityModifiedException(entityId, "Entities cannot be modified multiple times in a single processsor.");
        }

        // Resolve target archetype
        EntityArchetypeImpl target = this.targetArchetypes.get(transmutation.getId());
        if (target == null) {
            target = entityIndex.getArchetype(this, transmutation);
            this.targetArchetypes.set(transmutation.getId(), target);
        }

        // Prepare result
        TransitionImpl transition = transitions.getInstance();
        transition.archetype = target;

        // Return early if no archetype change
        if (target == this) {
            transition.index = index;
            return transition;
        }

        // Mark archetype dirty
        markDirty(DIRTY_MOVE);

        // Add entity to target archetype
        int entityId = this.entities.get(index);
        int targetIndex = transition.index = target.addEntity(entityId);

        // Copy components to target archetype
        ArchetypeMover mover = this.movers.get(target.id);
        mover.moveEntity(index, targetIndex);

        // Track changed entity
        this.changedEntitiesLookup.set(index, target.id);
        this.movedEntitiesLookup.get(target.id).add(index, targetIndex);

        // Move references
        Bag<EntityRefImpl> refs = this.references.get(index);
        if (refs != null && !refs.isEmpty()) {
            Bag<EntityRefImpl> targetRefs = target.getEntityRefs(targetIndex);

            EntityRefImpl[] data = refs.getData();
            for (int i = 0, s = refs.size(); i < s; i++) {
                targetRefs.add(data[i].set(target, targetIndex, entityId));
            }

            refs.clear();
        }

        return transition;
    }

    private int addEntity(int entityId) {
        int index;
        synchronized (this.entities) {
            index = this.entities.size();

            // Ensure capacity for data
            if (index + 1 >= this.entities.getCapacity()) {
                this.dataLock.lock();

                try {
                    if (index >= this.entities.getCapacity()) {
                        this.entities.ensureCapacity(index);
                        this.references.ensureCapacity(index);
                        this.data.ensureCapacity(index);
                    }
                } finally {
                    this.dataLock.unlock();
                }
            }

            // Retrieve entity id
            this.entities.setUnsafe(index, entityId);
        }

        this.moved.compareAndSet(-1, index);

        return index;
    }

    private void markDirty(int value) {
        if (value <= this.dirty) {
            return;
        }

        synchronized (this) {
            if (value <= this.dirty) {
                return;
            }

            if (value == DIRTY_MOVE) {
                if (this.dirty == DIRTY) {
                    this.entityIndex.unmarkDirty(this);
                }

                this.entityIndex.markDirtyMoved(this);
            } else {
                this.entityIndex.markDirty(this);
            }

            this.dirty = value;
        }
    }

    public final void process() {
        // TODO lock writing while processing, e.g. when process triggered by system A and system B runs in parallel
        this.dirty = 0;

        processCreated();
        processDeleted();

        // Reset change lookup
        changedEntitiesLookup.clear();
    }

    public final void processMoved() {
        // TODO lock writing while processing, e.g. when process triggered by system A and system B runs in parallel
        this.dirty = 0;

        processCreated();
        processMovedEntities();
        processDeleted();

        // Reset change lookup
        changedEntitiesLookup.clear();
    }

    private void processCreated() {
        int created = this.created.getAndSet(-1);
        int moved = this.moved.getAndSet(-1);

        // Notify inserted listeners for created entities (added..moved)
        if (created > -1) {
            int until = moved > created ? moved : entities.size();

            EntityArchetypeListener[] inserted = this.inserted.getData();
            for (int i = 0, s = this.inserted.size(); i < s; i++) {
                inserted[i].process(created, until);
            }
        }

        // Notify inserted listeners for created entities (after moved)
        if (!this.createdEntities.isEmpty()) {
            EntityArchetypeListener[] inserted = this.inserted.getData();
            for (int i = 0, s = this.inserted.size(); i < s; i++) {
                inserted[i].process(this.createdEntities);
            }

            this.createdEntities.clear();
        }
    }

    private void processMovedEntities() {
        int s = movedEntities.size();
        if (s == 0) {
            return;
        }

        MovedEntities[] data = movedEntities.getData();
        for (int i = 0; i < s; i++) {
            data[i].process();
        }

        this.movedEntities.clear();
    }

    private void processDeleted() {
        if (deletedEntities.isEmpty()) {
            return;
        }

        validateState();

        // Notify removed listeners before deletion
        EntityArchetypeListener[] removed = this.removed.getData();
        for (int i = 0, s = this.removed.size(); i < s; i++) {
            removed[i].process(deletedEntities);
            validateState();
        }

        // Delete each entity by moving data of last entity to its slot 
        int[] data = deletedEntities.getData();
        for (int i = deletedEntities.size() - 1; i >= 0; i--) {
            int index = data[i];

            // Invalidate references
            Bag<EntityRefImpl> refs = getEntityRefs(index);
            if (!refs.isEmpty()) {
                EntityRefImpl[] refData = refs.getData();
                for (int r = 0, rs = refs.size(); r < rs; r++) {
                    refData[r].reset(); // don't return to pool, has to be done by the user
                    validateState();
                }

                refs.clear();
            }

            // Clear data if last entity removed
            int lastIndex = this.entities.size() - 1;
            if (index == lastIndex) {
                int entityId = this.entities.removeLast();
                this.indices.set(entityId, -1);
                this.entityIndex.freeEntityId(entityId);
                this.data.removeAndFreeLastComponents();

                continue;
            }

            // Move data from last entity to removed slot
            this.data.removeAndFreeComponents(index);

            int lastEntityId = this.entities.get(lastIndex);

            // Update references of last last entity
            Bag<EntityRefImpl> lastRefs = getEntityRefs(lastIndex);
            if (!lastRefs.isEmpty()) {
                EntityRefImpl[] refData = lastRefs.getData();
                for (int r = 0, rs = lastRefs.size(); r < rs; r++) {
                    refs.add(refData[r].set(index, lastEntityId));
                }

                lastRefs.clear();
            }

            // Update indices and entities
            int entityId = this.entities.get(index);
            this.indices.set(entityId, -1);
            this.entityIndex.freeEntityId(entityId);

            this.entities.removeLast();
            this.indices.set(lastEntityId, index);
            this.entities.set(index, lastEntityId);
        }

        // Clear state
        deletedEntities.clear();
        validateState();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("EntityArchetypeImpl(")
                .append("id = ").append(this.id).append(", ")
                .append("alive = ").append(getAlive()).append(", ")
                .append("added = ").append(this.entities.size() - getAlive()).append(", ")
                .append("components = ").append(components)
                .append(")")
                .toString();
    }

    private Lock getLock(int index) {
        int idx = index & (lockSize - 1);
        return this.locks[idx];
    }

    private static final class ArchetypeMover {

        private final EntityArchetypeData sourceData;

        private final EntityArchetypeImpl target;
        private final EntityArchetypeData targetData;

        private final int[] mapping;
        private final int[] create;
        private final boolean[] free;

        public ArchetypeMover(EntityArchetypeImpl source, EntityArchetypeImpl target) {
            this.sourceData = source.data;

            this.target = target;
            this.targetData = target.data;

            this.mapping = source.components.stream()
                    .mapToInt(target.components::indexOf)
                    .toArray();

            this.create = IntStream.range(0, target.components.size())
                    .filter(i -> !source.components.contains(target.components.get(i)) && Components.getMetadata(target.components.get(i)).getPool() != null)
                    .toArray();

            this.free = new boolean[target.components.size()];
            for (int i = 0, s = target.components.size(); i < s; i++) {
                Class<?> clazz = target.components.get(i);
                this.free[i] = !source.components.contains(clazz) && Components.getMetadata(clazz).getPool() != null;
            }
        }

        public void moveEntity(int sourceIndex, int targetIndex) {
            this.sourceData.moveEntity(sourceIndex, this.targetData, targetIndex, this.mapping, this.create);
        }

        public void deleteAddedEntity(int entityId) {
            int targetIndex = -1;

            // Get index of entity
            int[] data = target.entities.getData();
            for (int i = target.created.get(), s = target.entities.size(); i < s; i++) {
                if (data[i] == entityId) {
                    targetIndex = i;
                    break;
                }
            }

            // Clear slot, remove from entities and data
            this.target.entities.set(targetIndex, -1);
            this.targetData.removeComponents(targetIndex, this.free);
        }

    }

    private final class MovedEntities {

        private final EntityArchetypeImpl archetype;
        private final IntBag indices;
        private final IntBag targetIndices;

        private final Bag<EntityArchetypeListener> inserted = new Bag<>(EntityArchetypeListener.class, 4);
        private final Bag<EntityArchetypeListener> removed = new Bag<>(EntityArchetypeListener.class, 4);

        private final AtomicBoolean added = new AtomicBoolean(false);

        public MovedEntities(EntityArchetypeImpl archetype) {
            this.archetype = archetype;
            this.indices = new IntBag(256);
            this.targetIndices = new IntBag(256);
        }

        public void process() {
            // Clear moved flag
            archetype.moved.set(-1);

            // Notify removed listeners of this archetype
            EntityArchetypeListener[] removed = this.removed.getData();
            for (int i = 0, s = this.removed.size(); i < s; i++) {
                removed[i].process(this.indices);
            }

            int[] deletedEntities = EntityArchetypeImpl.this.deletedEntities.getData();

            // Remove entities from source archetype
            while (!this.indices.isEmpty()) {
                int index = this.indices.removeLast();

                // Clear references
                Bag<EntityRefImpl> refs = getEntityRefs(index);
                refs.clear();

                // Clear data if last entity moved
                int lastIndex = entities.size() - 1;
                if (index == lastIndex) {
                    int entityId = EntityArchetypeImpl.this.entities.removeLast();
                    EntityArchetypeImpl.this.indices.set(entityId, -1);
                    EntityArchetypeImpl.this.changedEntitiesLookup.set(index, -1);
                    EntityArchetypeImpl.this.data.removeLastComponents();

                    continue;
                }

                // Move data from last entity to moved slot
                EntityArchetypeImpl.this.data.removeComponents(index);

                // Update indices and entities
                int entityId = EntityArchetypeImpl.this.entities.get(index);
                EntityArchetypeImpl.this.indices.set(entityId, -1);

                int lastEntityId = EntityArchetypeImpl.this.entities.removeLast();
                EntityArchetypeImpl.this.indices.set(lastEntityId, index);
                EntityArchetypeImpl.this.entities.set(index, lastEntityId);

                int change = EntityArchetypeImpl.this.changedEntitiesLookup.get(lastIndex);
                EntityArchetypeImpl.this.changedEntitiesLookup.set(index, change);
                EntityArchetypeImpl.this.changedEntitiesLookup.set(lastIndex, -1);

                // Update index if last entity marked for deletion
                if (change <= DELETED) {
                    int deletedIndex = -change - DELETED;

                    // Shift elements to the right
                    while (deletedIndex > 0 && deletedEntities[deletedIndex - 1] > index) {
                        deletedEntities[deletedIndex] = deletedEntities[deletedIndex - 1];
                        deletedIndex--;
                    }

                    deletedEntities[deletedIndex] = index;
                    continue;
                }

                // Update references of last last entity
                Bag<EntityRefImpl> lastRefs = getEntityRefs(lastIndex);
                if (!lastRefs.isEmpty()) {
                    EntityRefImpl[] refData = lastRefs.getData();
                    for (int r = 0, rs = lastRefs.size(); r < rs; r++) {
                        refs.add(refData[r].set(index, lastEntityId));
                    }

                    lastRefs.clear();
                }

                // Update index if last entity marked for move to another archetype
                if (change > -1) {
                    MovedEntities entities = movedEntitiesLookup.get(change);
                    entities.update(lastIndex, index);
                }
            }

            // Notify inserted listeners of target archetype
            EntityArchetypeListener[] inserted = this.inserted.getData();
            for (int i = 0, s = this.inserted.size(); i < s; i++) {
                inserted[i].process(this.targetIndices);
            }

            // Reset state
            this.targetIndices.clear();
            this.added.set(false);
        }

        public void add(int index, int targetIndex) {
            // Add to movedEntities on first add 
            if (added.compareAndSet(false, true)) {
                movedEntities.add(this);
            }

            // Add target index
            this.targetIndices.add(targetIndex);

            // Determine index to insert to keep indices sorted
            int[] data = indices.getData();
            for (int i = 0, s = movedEntities.size(); i < s; i++) {
                int value = data[i];
                if (index < value) {
                    indices.insert(i, index);
                    return;
                }
            }

            // Add index otherwise
            indices.add(index);
        }

        public void remove(int index) {
            this.indices.removeIndex(index);

            if (this.indices.isEmpty() && added.compareAndSet(true, false)) {
                movedEntities.removeIdentity(this);
            }
        }

        private void update(int index, int newIndex) {
            int[] indices = this.indices.getData();
            int pos = Arrays.binarySearch(indices, 0, this.indices.size(), index); // assume not -1, that'd be a bug elsewhere

            // Shift elements to the right
            while (pos > 0 && indices[pos - 1] > index) {
                indices[pos] = indices[pos - 1];
                pos--;
            }

            indices[pos] = newIndex;
        }

    }

    private final class TransitionImpl implements Transition, Pooled {

        private EntityArchetypeImpl archetype;
        private int index = -1;

        @Override
        public EntityArchetype archetype() {
            return archetype;
        }

        @Override
        public int index() {
            return this.index;
        }

        @Override
        public void free() {
            EntityArchetypeImpl.this.transitions.free(this);
        }

        @Override
        public void reset() {
            this.archetype = null;
            this.index = -1;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TransitionImpl [archetype=").append(this.archetype).append(", index=").append(this.index).append("]");
            return builder.toString();
        }

    }

    @Override
    public final EntityRefImpl createEntityRef(int index) {
        EntityRefImpl ref = referencePool.getInstance().set(this, index, this.entities.get(index));

        Bag<EntityRefImpl> refs = getEntityRefs(index);
        refs.add(ref);

        return ref;
    }

    private Bag<EntityRefImpl> getEntityRefs(int index) {
        Bag<EntityRefImpl> refs = this.references.get(index);
        if (refs != null) {
            return refs;
        }

        Lock lock = getLock(index);
        lock.lock();

        try {
            refs = this.references.get(index);
            if (refs != null) {
                return refs;
            }

            refs = new Bag<>(EntityRefImpl.class, 4);
            this.references.set(index, refs);

            return refs;
        } finally {
            lock.unlock();
        }
    }

    public final void freeEntityRef(EntityRefImpl ref) {
        this.references.get(ref.index).remove(ref);
        this.referencePool.free(ref);
    }

}
