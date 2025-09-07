package de.schosin.decs.api.builder;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Singleton;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.builder.system.SystemBuilder;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.utils.locks.YieldingSpinLock;

/**
 * Builder for {@link World}.
 * 
 * <h3>Processors (Systems)</h3>
 *  
 * To implement systems, you can declare methods anywhere you like and annotate them with {@link EntityProcessor @EntityProcessor} or {@link SystemProcessor @SystemProcessor}.
 * The allowed parameter types of the methods are described in the documentation of {@link EntityProcessor @EntityProcessor} and {@link SystemProcessor @SystemProcessor}.
 * The return type for the methods should be {@code void}.
 * For every {@link EntityProcessor @EntityProcessor} or {@link SystemProcessor @SystemProcessor} a type with the same name as in the annotation will be generated, as well as another type with the "Ref" suffix.
 * 
 * <p>
 * All systems passed to {@link WorldBuilder} or its subtypes will be run in order.
 */
public interface WorldBuilder {

    static int DEFAULT_ENTITIES_LOCK_SIZE = 1024;

    /**
     * Creates an instance of {@link WorldBuilder}.
     * 
     * @return this instance
     */
    static WorldBuilder create() {
        try {
            return (WorldBuilder) Class.forName("de.schosin.decs.core.builder.CoreWorldBuilder").getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() != null && ex.getTargetException().getClass().getName().startsWith("de.schosin")) {
                throw (RuntimeException) ex.getTargetException();
            }

            throw new IllegalArgumentException(String.format("Exception creating WorldBuilder instance: %s", ex.getMessage()), ex);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException ex) {
            throw new IllegalArgumentException(String.format("Exception creating WorldBuilder instance: %s", ex.getMessage()), ex);
        }
    }

    /**
     * Applies the consumer to this instance.
     * Can be used to implement systems by either implementing {@link Consumer Comsumer<WorldBuilder>} or passing a method reference of a method accepting {@link WorldBuilder}.
     * 
     * {@snippet:
     * MyOtherSystem otherSystem = new MyOtherSystem();
     * World world = World.builder()
     *         .with(MySystem::new)
     *         .with(otherSystem::attach)
     *         .build();
     * 
     * ...
     * 
     * class MySystem {
     * 
     *     MySystem(WorldBuilder builder) {
     *         // initialize system, register processors or compositions, add singletons, ...
     *     }
     * 
     * }
     * 
     * class MyOtherSystem {
     * 
     *     MyOtherSystem() {
     *         // initialize state not managed by the world
     *     }
     *     
     *     public void attach(WorldBuilder builder) {
     *         // initialize system, register processors or compositions, add singletons, ...
     *     }
     * 
     * }
     * }
     * 
     * @param consumer consumer of this instance
     * @return this builder
     */
    WorldBuilder with(Consumer<WorldBuilder> consumer);

    /**
     * Defines the default executor used by subsequent {@link #parallel(Consumer...)} calls.
     * Previous calls to {@link #parallel(Consumer...)} will not be affected.
     * 
     * @param executor default executor
     * @return this instance
     */
    WorldBuilder executor(ExecutorService executor);

    /**
     * Configure processors that run in parallel.
     * Any processors declared on the {@link ProcessBuilder} will be run in parallel.
     * 
     * <p>
     * This will use the {@link ExecutorService} declared in {@link #executor(ExecutorService)}.
     * If none is declared, it will default to {@link ForkJoinPool#commonPool()}.
     * 
     * @param consumer of {@link ProcessBuilder}
     * @return this builder
     */
    WorldBuilder parallel(Consumer<SystemBuilder> consumer);

    /**
     * Configure processors that run in parallel with the given {@link ExecutorService}.
     * Any processors declared on the {@link ProcessBuilder} will be run in parallel.
     * 
     * @param executor executor service
     * @param consumer consumer of {@link ProcessBuilder}
     * @return this builder
     */
    WorldBuilder parallel(ExecutorService executor, Consumer<SystemBuilder> consumer);

    /**
     * Adds the systems to the world.
     * The passed types must each contain atleast one {@link de.schosin.decs.api.annotations.system system annotation}.
     * 
     * <p>
     * Added systems will be run in the same order as they were added.
     * When this method is called multiple times, the new systems will be run after all previously added systems.
     * 
     * @param systems systems to add
     * @return this builder
     * @throws InvalidSystemException if a passed type is not a valid system
     * @throws DuplicateSystemException if a passed system has already been registered
     * @throws MissingSystemDependencyException if a system has a dependency on another system that has not been registered yet
     */
    WorldBuilder add(Class<?>... systems);

    /**
     * Add a singleton to the world that can be used by systems with {@link Singleton @Singleton}.
     * 
     * @param singleton singleton instance
     * @return this builder
     * @throws DuplicateSingletonException when type already registered
     */
    WorldBuilder singleton(Object singleton);

    /**
     * Configures the locks used for synchronizing entity data.
     * 
     * <p>
     * All entities within an {@link EntityArchetype} share these locks.
     * The supplier must be stateless, as it will be called {@code lockSize} times for every unique archetype.
     * 
     * <p>
     * For java 8 the default values are {@link #DEFAULT_ENTITIES_LOCK_SIZE 1024} and {@link YieldingSpinLock}.
     * If "decs-core-11" is on the classpath, the default values are {@link #DEFAULT_ENTITIES_LOCK_SIZE 1024} and {@link de.schosin.decs.core.java11.utils.locks.WaitingSpinLock}.
     * 
     * <p>
     * The default lock can be overridden by providing a EntitiesLockSupplierFactory SPI from the "decs-core" dependency.
     * 
     * @param lockSize number of locks to use, must be a power of 2
     * @param lockSupplier supplier of lock instance
     * @return this builder
     * @throws IllegalArgumentException if lockSize is not a positive power of two
     */
    WorldBuilder entitiesLocks(int lockSize, Supplier<Lock> lockSupplier);

    /**
     * Build the world instance.
     * 
     * @return world instance
     */
    World build();

}
