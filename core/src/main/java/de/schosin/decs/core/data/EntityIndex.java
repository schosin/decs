package de.schosin.decs.core.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.api.entities.Transmutation;
import de.schosin.decs.api.internal.LockConfig;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.api.utils.collections.IntBag;
import de.schosin.decs.core.CoreWorld;
import de.schosin.decs.values.ComponentMetadata;
import de.schosin.decs.values.Components;

public final class EntityIndex {

    // TODO configuration parameter
    private static final int MAX_PROCESS_ATTEMPTS = 4;

    private final CoreWorld world;
    private final ComponentIndex componentIndex;

    private final int lockSize;
    private final Supplier<Lock> lockSupplier;

    private final AtomicInteger nextEntityId = new AtomicInteger(1);
    private final IntBag entityIds = new IntBag();

    private final Map<BitSet, EntityArchetypeImpl> archetypeLookup = new HashMap<>();
    private final Bag<EntityArchetypeImpl> archetypes = new Bag<>(EntityArchetypeImpl.class, 8);
    private final Collection<EntityArchetypeImpl> immutableArchetypes = Collections.unmodifiableCollection(this.archetypeLookup.values());

    private final List<Callback> inserted = new ArrayList<>();
    private final List<Callback> removed = new ArrayList<>();

    private final Dirty dirty = new Dirty();

    public EntityIndex(CoreWorld world, ComponentIndex componentIndex, LockConfig lockConfig) {
        this.world = world;
        this.componentIndex = componentIndex;

        this.lockSize = lockConfig.getEntitiesLockSize();
        this.lockSupplier = lockConfig.getEntitiesLockSupplier();
    }

    public Collection<EntityArchetypeImpl> getArchetypes() {
        return immutableArchetypes;
    }

    public EntityArchetypeImpl getArchetype(int[] ids, List<Class<?>> components) {
        BitSet bitset = new BitSet();
        for (int id : ids) {
            bitset.set(id);
        }

        EntityArchetypeImpl archetype = archetypeLookup.get(bitset);
        if (archetype != null) {
            return archetype;
        }

        return createArchetype(bitset, components);
    }

    public EntityArchetypeImpl getArchetype(EntityArchetypeImpl source, Transmutation transmutation) {
        List<ComponentMetadata<?>> metadata = new ArrayList<>();
        for (Class<?> component : source.getComponents()) {
            metadata.add(Components.getMetadata(component));
        }

        for (ComponentMetadata<?> component : transmutation.getAdd()) {
            metadata.add(component);
        }

        for (ComponentMetadata<?> component : transmutation.getRemove()) {
            metadata.remove(component);
        }

        Collections.sort(metadata, ComponentMetadata.COMPARATOR);

        BitSet bitset = new BitSet();

        for (ComponentMetadata<?> component : metadata) {
            bitset.set(component.getId());
        }

        EntityArchetypeImpl archetype = archetypeLookup.get(bitset);
        if (archetype != null) {
            return archetype;
        }

        List<Class<?>> components = metadata.stream().map(ComponentMetadata::getType).collect(Collectors.toList());
        return createArchetype(bitset, components);
    }

    private synchronized EntityArchetypeImpl createArchetype(BitSet bitset, List<Class<?>> components) {
        EntityArchetypeImpl archetype = archetypeLookup.get(bitset);
        if (archetype != null) {
            return archetype;
        }

        archetype = new EntityArchetypeImpl(archetypeLookup.size(), world, this, componentIndex, components, this.lockSize, this.lockSupplier);

        for (EntityArchetypeImpl other : archetypes) {
            archetype.offerArchetype(other);
            other.offerArchetype(archetype);
        }

        archetypeLookup.put(bitset, archetype);
        archetypes.set(archetype.id(), archetype);
        dirty.ensureCapacity(archetype.id());

        world.handleNewArchetype(archetype);

        return archetype;
    }

    public EntityArchetypeImpl getArchetype(int id) {
        return this.archetypes.get(id);
    }

    public void inserted(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory) {
        Callback callback = new Callback(composition, factory);
        this.inserted.add(callback);

        for (EntityArchetypeImpl archetype : archetypes) {
            archetype.offerInserted(callback);
        }
    }

    public void removed(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory) {
        Callback callback = new Callback(composition, factory);
        this.removed.add(callback);

        for (EntityArchetypeImpl archetype : archetypes) {
            archetype.offerRemoved(callback);
        }
    }

    public int getEntityId() {
        if (entityIds.size() == 0) {
            return nextEntityId.getAndIncrement();
        }

        return entityIds.removeLast();
    }

    public void freeEntityId(int entityId) {
        entityIds.add(entityId);
    }

    public void markDirty(EntityArchetypeImpl archetype) {
        this.dirty.add(archetype);
    }

    public void unmarkDirty(EntityArchetypeImpl archetype) {
        this.dirty.remove(archetype);
    }

    public void markDirtyMoved(EntityArchetypeImpl archetype) {
        this.dirty.addMoved(archetype);
    }

    public void process() {
        int attempts = MAX_PROCESS_ATTEMPTS;

        do {
            // Swap dirty bags in case further modifications happen during processing
            Bag<EntityArchetypeImpl> dirty = this.dirty.dirty;
            Bag<EntityArchetypeImpl> dirtyMoved = this.dirty.dirtyMoved;
            this.dirty.swap();

            int s = dirty.size(), sm = dirtyMoved.size();
            if (s == 0 && sm == 0) {
                return;
            }

            EntityArchetypeImpl[] data = dirty.getData();
            for (int i = 0; i < s; i++) {
                data[i].process();
            }

            EntityArchetypeImpl[] dataMoved = dirtyMoved.getData();
            for (int i = 0; i < sm; i++) {
                dataMoved[i].processMoved();
            }

            dirty.clear();
            dirtyMoved.clear();
        } while (--attempts > 0 && this.dirty.isDirty());
    }

    private static final class Dirty {

        private Bag<EntityArchetypeImpl> dirty = new Bag<>(EntityArchetypeImpl.class, 16);
        private Bag<EntityArchetypeImpl> dirtyMoved = new Bag<>(EntityArchetypeImpl.class, 16);

        private Bag<EntityArchetypeImpl> dirtyOverflow = new Bag<>(EntityArchetypeImpl.class, 16);
        private Bag<EntityArchetypeImpl> dirtyMovedOverflow = new Bag<>(EntityArchetypeImpl.class, 16);

        private void swap() {
            Bag<EntityArchetypeImpl> dirty = this.dirty;
            Bag<EntityArchetypeImpl> dirtyMoved = this.dirty;

            this.dirty = this.dirtyOverflow;
            this.dirtyMoved = this.dirtyMovedOverflow;

            this.dirtyOverflow = dirty;
            this.dirtyMovedOverflow = dirtyMoved;
        }

        public boolean isDirty() {
            return !this.dirty.isEmpty() || !this.dirtyMoved.isEmpty();
        }

        public void add(EntityArchetypeImpl archetype) {
            this.dirty.add(archetype);
        }

        public void addMoved(EntityArchetypeImpl archetype) {
            this.dirtyMoved.add(archetype);
        }

        public void remove(EntityArchetypeImpl archetype) {
            this.dirty.remove(archetype);
        }

        public void ensureCapacity(int index) {
            this.dirty.ensureCapacity(index);
            this.dirtyMoved.ensureCapacity(index);
            this.dirtyOverflow.ensureCapacity(index);
            this.dirtyMovedOverflow.ensureCapacity(index);
        }

    }

}
