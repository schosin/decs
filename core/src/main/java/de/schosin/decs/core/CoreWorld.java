package de.schosin.decs.core;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.api.exceptions.InvalidUtilityException;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.internal.LockConfig;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.systems.UtilityType;
import de.schosin.decs.api.utils.collections.CollectionUtils;
import de.schosin.decs.api.utils.collections.EntityBag;
import de.schosin.decs.api.utils.pool.Pool;
import de.schosin.decs.core.data.ComponentIndex;
import de.schosin.decs.core.data.EntityArchetypeImpl;
import de.schosin.decs.core.data.EntityBagImpl;
import de.schosin.decs.core.data.EntityIndex;
import de.schosin.decs.core.systems.ParallelSystemData;
import de.schosin.decs.core.systems.ParallelSystemType;
import de.schosin.decs.core.systems.SystemData;
import de.schosin.decs.core.systems.SystemDataImpl;
import de.schosin.decs.values.Types;
import de.schosin.decs.values.Values;

public final class CoreWorld implements InternalWorld {

    private final ComponentIndex componentIndex;
    private final EntityIndex entityIndex;

    private final Map<Class<?>, Object> singletons;
    private final Map<Class<?>, UtilityType> utilityLookup;
    private final Map<Class<?>, SystemType> systemLookup;
    private final Values values;

    private final int size;
    private final SystemType[] systems;

    private final Pool<EntityBagImpl> entityBags;

    @SuppressWarnings("unchecked")
    public CoreWorld(List<SystemData> systems, Map<Class<?>, Object> singletons, LockConfig lockConfig) {
        if (systems.isEmpty()) {
            throw new IllegalArgumentException("Cannot create world with no systems. Add atleast one system with WorldBuilder#add(Class[]).");
        }

        this.componentIndex = new ComponentIndex();
        this.entityIndex = new EntityIndex(this, componentIndex, lockConfig);

        this.singletons = CollectionUtils.mapOf(singletons);
        // TODO consider to make (unused) utilities lazy, as they initialize archetypes that might not be needed at runtime
        this.utilityLookup = new HashMap<>((Map<Class<?>, UtilityType>) (Map<?, ?>) Types.getUtilities(this));
        this.systemLookup = new HashMap<>();
        this.values = new Values();

        this.size = systems.size();
        this.systems = systems.stream()
                .map(this::buildSystem)
                .toArray(SystemType[]::new);

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

    private SystemType buildSystem(SystemData data) {
        if (data instanceof SystemDataImpl) {
            return buildSystem((SystemDataImpl) data);
        }

        return buildSystem((ParallelSystemData) data);
    }

    private SystemType buildSystem(SystemDataImpl data) {
        SystemType system = data.getInstance(this);
        systemLookup.put(data.metadata().clazz(), system);

        return system;
    }

    private SystemType buildSystem(ParallelSystemData data) {
        return new ParallelSystemType(data.executor(), data.systems().stream()
                .map(this::buildSystem)
                .collect(Collectors.toList()));
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
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
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
    public void process() {
        entityIndex.process();

        SystemType[] systems = this.systems;
        for (int i = 0, s = size; i < s; i++) {
            systems[i].runSystem();
            entityIndex.process();
        }
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
