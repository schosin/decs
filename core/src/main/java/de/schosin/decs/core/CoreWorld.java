package de.schosin.decs.core;

import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.api.exceptions.InvalidUtilityException;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.systems.UtilityType;
import de.schosin.decs.api.utils.collections.CollectionUtils;
import de.schosin.decs.api.utils.collections.EntityBag;
import de.schosin.decs.api.utils.locks.Locks;
import de.schosin.decs.api.utils.pool.Pool;
import de.schosin.decs.core.data.ComponentIndex;
import de.schosin.decs.core.data.EntityArchetypeImpl;
import de.schosin.decs.core.data.EntityBagImpl;
import de.schosin.decs.core.data.EntityIndex;
import de.schosin.decs.values.Types;
import de.schosin.decs.values.Values;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class CoreWorld implements InternalWorld {

    private final ComponentIndex componentIndex;
    private final EntityIndex entityIndex;

    private final Map<Class<?>, Object> singletons;
    private final Map<Class<?>, UtilityType> utilityLookup;
    private final Map<Class<?>, SystemType> systemLookup;
    private final Values values;
    private final Locks locks;

    private final SystemInvocation systemInvocation;
    private final SystemType[] systems;

    private final Pool<EntityBagImpl> entityBags;

    @SuppressWarnings("unchecked")
    public CoreWorld(Function<InternalWorld, SystemInvocation> systemInvocation, Map<Class<?>, Object> singletons) {
        this.componentIndex = new ComponentIndex();
        this.entityIndex = new EntityIndex(this, componentIndex);

        this.singletons = CollectionUtils.mapOf(singletons);
        // TODO consider to make (unused) utilities lazy, as they initialize archetypes that might not be needed at runtime
        this.utilityLookup = new HashMap<>((Map<Class<?>, UtilityType>) (Map<?, ?>) Types.getUtilities(this));
        this.values = new Values();
        this.locks = Locks.getInstance(this);

        this.systemInvocation = systemInvocation.apply(this);
        this.systems = this.systemInvocation.getSystems();

        this.systemLookup = new HashMap<>();
        for (SystemType system : systems) {
            this.systemLookup.put(system.getClass().getSuperclass(), system);
        }

        for (EntityArchetypeImpl archetype : entityIndex.getArchetypes()) {
            for (SystemType system : this.systems) {
                system.offerArchetype(archetype);
            }

            for (UtilityType utility : this.utilityLookup.values()) {
                utility.offerArchetype(archetype);
            }
        }

        this.entityBags = Pool.unbounded(32, EntityBagImpl.class, () -> new EntityBagImpl(this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSystem(Class<T> clazz) {
        return (T) systemLookup.get(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getUtility(Class<T> clazz) {
        T instance = (T) utilityLookup.get(clazz);
        if (instance != null) {
            return instance;
        }

        // TODO remove if APT can read META-INF from dependencies
        synchronized (this.utilityLookup) {
            instance = (T) utilityLookup.get(clazz);
            if (instance != null) {
                return instance;
            }

            String implementationName = clazz.getPackage().getName() + "." + clazz.getSimpleName() + "Impl";
            try {
                Class<?> implementation = Class.forName(implementationName);
                if (!clazz.isAssignableFrom(implementation)) {
                    throw new InvalidUtilityException(String.format("Type '%s' does not implement utility interface '%s'", implementationName, clazz.getName()), clazz);
                }

                if (!UtilityType.class.isAssignableFrom(implementation)) {
                    throw new InvalidUtilityException(String.format("Type '%s' is not a generated utility interface implementation for '%s'", implementationName, clazz.getName()), clazz);
                }

                instance = (T) implementation.getDeclaredConstructor(InternalWorld.class).newInstance(this);
                utilityLookup.put(clazz, (UtilityType) instance);

                return instance;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     IllegalArgumentException | InvocationTargetException | NoSuchMethodException |
                     SecurityException ex) {
                throw new InvalidUtilityException(String.format("Failed to instantiate implementation '%s' for utility interface '%s'", implementationName, clazz.getName()), ex, clazz);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSingleton(Class<T> clazz) {
        return (T) this.singletons.get(clazz);
    }

    @Override
    public Values getValues() {
        return this.values;
    }

    @Override
    public EntityBag createEntityBag() {
        return this.entityBags.getInstance();
    }

    public void freeEntityBag(EntityBagImpl entityBag) {
        this.entityBags.free(entityBag);
    }

    @Override
    public void flushChanges() {
        entityIndex.process();
    }

    @Override
    public void enableSystemGroup(String name) {
        this.systemInvocation.enableSystemGroup(name);
    }

    @Override
    public void disableSystemGroup(String name) {
        this.systemInvocation.disableSystemGroup(name);
    }

    @Override
    public void process() {
        this.entityIndex.process();
        this.systemInvocation.runSystems();
    }

    @Override
    public EntityArchetype getEntityArchetype(Class<?>... components) {
        // Sort components array in-place and return their ids
        int[] ids = componentIndex.sort(components);

        return entityIndex.getArchetype(ids, CollectionUtils.listOf(components));
    }

    @Override
    public void inserted(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory) {
        this.entityIndex.inserted(composition, factory);
    }

    @Override
    public void removed(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory) {
        this.entityIndex.removed(composition, factory);
    }

    @Override
    public Locks getLocks() {
        return this.locks;
    }

    public void handleNewArchetype(EntityArchetypeImpl archetype) {
        if (this.systems == null) {
            return;
        }

        for (SystemType system : systems) {
            system.offerArchetype(archetype);
        }

        for (UtilityType utility : this.utilityLookup.values()) {
            utility.offerArchetype(archetype);
        }
    }

}
