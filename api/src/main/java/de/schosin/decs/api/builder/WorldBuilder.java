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
import de.schosin.decs.api.exceptions.DuplicateSingletonException;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.api.utils.locks.YieldingSpinLock;

/**
 * Builder for {@link World}.
 *
 * <h3>Processors (Systems)</h3>
 * <p>
 * To implement systems, you can declare methods anywhere you like and annotate them with {@link EntityProcessor @EntityProcessor} or {@link SystemProcessor @SystemProcessor}.
 * The allowed parameter types of the methods are described in the documentation of {@link EntityProcessor @EntityProcessor} and {@link SystemProcessor @SystemProcessor}.
 * The return type for the methods should be {@code void}.
 * For every {@link EntityProcessor @EntityProcessor} or {@link SystemProcessor @SystemProcessor} a type with the same name as in the annotation will be generated, as well as another type with the "Ref" suffix.
 *
 * <p>
 * All systems passed to {@link WorldBuilder} or its subtypes will be run in order.
 */
public interface WorldBuilder {

    /**
     * Creates an instance of {@link WorldBuilder}.
     *
     * @return this instance
     */
    static WorldBuilder create(Class<? extends SystemInvocation> systemInvocation) {
        try {
            return (WorldBuilder) Class.forName("de.schosin.decs.core.builder.CoreWorldBuilder").getDeclaredConstructor(Class.class).newInstance(systemInvocation);
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
     * Add a singleton to the invocation that can be used by systems with {@link Singleton @Singleton}.
     *
     * @param singleton singleton instance
     * @return this builder
     * @throws DuplicateSingletonException when type already registered
     */
    WorldBuilder singleton(Object singleton);

    /**
     * Build the invocation instance.
     *
     * @return invocation instance
     */
    World build();

}
