package de.schosin.decs.api.builder.system;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import de.schosin.decs.api.builder.WorldBuilder;

public interface SystemBuilder {

    /**
     * Defines the default executor used by subsequent {@link #parallel(Consumer...)} calls.
     * Previous calls to {@link #parallel(Consumer...)} will not be affected.
     * 
     * @param executor default executor
     * @return this instance
     */
    SystemBuilder executor(ExecutorService executor);

    /**
     * Applies the consumer to this instance.
     * Can be used to implement systems by either implementing {@link Consumer Comsumer<WorldBuilder>} or passing a method reference of a method accepting {@link WorldBuilder}.
     * 
     * {@snippet:
     * MyOtherSystem otherSystem = new MyOtherSystem();
     * World invocation = World.builder()
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
     *         // initialize state not managed by the invocation
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
    SystemBuilder with(Consumer<SystemBuilder> consumer);

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
    SystemBuilder parallel(Consumer<SystemBuilder> consumer);

    /**
     * Configure processors that run in parallel with the given {@link ExecutorService}.
     * Any processors declared on the {@link ProcessBuilder} will be run in parallel.
     * 
     * @param executor executor service
     * @param consumer consumer of {@link ProcessBuilder}
     * @return this builder
     */
    SystemBuilder parallel(ExecutorService executor, Consumer<SystemBuilder> consumer);

    /**
     * Adds the systems to the invocation.
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
    SystemBuilder add(Class<?>... systems);

}
