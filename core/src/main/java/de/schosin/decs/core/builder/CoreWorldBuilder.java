package de.schosin.decs.core.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.schosin.decs.api.World;
import de.schosin.decs.api.builder.WorldBuilder;
import de.schosin.decs.api.builder.system.SystemBuilder;
import de.schosin.decs.api.exceptions.DuplicateSingletonException;
import de.schosin.decs.api.exceptions.DuplicateSystemException;
import de.schosin.decs.api.exceptions.InvalidSystemException;
import de.schosin.decs.api.exceptions.MissingSystemDependencyException;
import de.schosin.decs.api.internal.LockConfig;
import de.schosin.decs.api.utils.locks.YieldingSpinLock;
import de.schosin.decs.core.CoreWorld;
import de.schosin.decs.core.builder.system.CoreSystemBuilder;
import de.schosin.decs.core.spi.EntitiesLockSupplierFactory;
import de.schosin.decs.core.systems.SystemData;
import de.schosin.decs.core.systems.SystemDataImpl;
import de.schosin.decs.values.SystemMetadata;
import de.schosin.decs.values.Types;

public final class CoreWorldBuilder implements WorldBuilder, CoreBuilder {

    private final List<SystemData> systems = new ArrayList<>();
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    private ExecutorService executor = ForkJoinPool.commonPool();

    private int entitiesLockSize = DEFAULT_ENTITIES_LOCK_SIZE;
    private Supplier<Lock> entitiesLockSupplier = resolveDefaultLockSupplier();

    private static Supplier<Lock> resolveDefaultLockSupplier() {
        Iterator<EntitiesLockSupplierFactory> iterator = ServiceLoader.load(EntitiesLockSupplierFactory.class).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return YieldingSpinLock::new;
    }

    @Override
    public boolean isSystemRegistered(Class<?> system) {
        return this.systems.stream().anyMatch(data -> CoreBuilder.matches(system, data));
    }

    @Override
    public boolean isSingletonRegistered(Class<?> singleton) {
        return this.singletons.containsKey(singleton);
    }

    @Override
    public WorldBuilder with(Consumer<WorldBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    @Override
    public WorldBuilder executor(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        return this;
    }

    @Override
    public WorldBuilder parallel(Consumer<SystemBuilder> consumer) {
        return parallel(executor, consumer);
    }

    @Override
    public WorldBuilder parallel(ExecutorService executor, Consumer<SystemBuilder> consumer) {
        CoreSystemBuilder builder = new CoreSystemBuilder(this, executor);
        consumer.accept(builder);

        this.systems.add(builder.build());
        return this;
    }

    @Override
    public WorldBuilder add(Class<?>... systems) {
        // TODO provide a way to initialize values in builder, before adding systems? does that help?
        // TODO provide a way to pass in system instances? or support @SystemProcessor-like constructors or a @Initialize method?

        for (Class<?> system : systems) {
            if (isSystemRegistered(system)) {
                throw new DuplicateSystemException(String.format("System '%s' already added to this world.", system.getName()), system);
            }

            // Get system metadata
            SystemMetadata metadata = Types.getSystemMetadata(system);
            if (metadata == null) {
                throw new InvalidSystemException(
                        String.format("System metadata for class '%s' is not available. Make sure the annotation processor \"decs-codegen\" is enabled.", system.getName()),
                        system);
            }

            // Validate singleton dependencies
            for (Class<?> dependency : metadata.dependencies()) {
                if (!isSingletonRegistered(dependency)) {
                    throw new MissingSystemDependencyException(
                            String.format("Dependency '%s' for system '%s' has not been added. Make sure dependencies are added before they are used.", dependency.getName(), system.getName()),
                            system);
                }
            }

            // Add system
            this.systems.add(new SystemDataImpl(metadata));
        }

        return this;
    }

    @Override
    public WorldBuilder singleton(Object singleton) {
        Class<?> clazz = singleton.getClass();

        if (this.singletons.containsKey(clazz)) {
            throw new DuplicateSingletonException(clazz);
        }

        this.singletons.put(clazz, singleton);
        return this;
    }

    @Override
    public WorldBuilder entitiesLocks(int lockSize, Supplier<Lock> lockSupplier) {
        if (lockSize < 2) {
            throw new IllegalArgumentException("lockSize must be 2 or higher");
        }

        boolean powerOf2 = (lockSize & (lockSize - 1)) == 0;
        if (!powerOf2) {
            throw new IllegalArgumentException("lockSize must be a power of 2, but was: " + lockSize);
        }

        this.entitiesLockSize = lockSize;
        this.entitiesLockSupplier = Objects.requireNonNull(lockSupplier, "lockSupplier must not be null");

        return this;
    }

    @Override
    public World build() {
        return new CoreWorld(systems, singletons, new LockConfig(this.entitiesLockSize, this.entitiesLockSupplier));
    }

}
