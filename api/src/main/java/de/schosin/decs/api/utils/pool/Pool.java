package de.schosin.decs.api.utils.pool;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

import de.schosin.decs.api.utils.collections.Bag;

/**
 * Simple object pool to avoid heap allocation and garbage collection at runtime.
 * Use the static factory methods to create a new pool.
 * 
 * @param <T> type of pooled type
 */
@NullMarked
public interface Pool<T> {

    /**
     * Returns a new unbounded pool.
     * The pool will be prefilled with up to {@code initialSize} elements.
     * 
     * <p>
     * If the pooled type implements {@link Pooled}, it will be automatically {@link Pooled#reset() reset} when returned to the pool.
     * 
     * @param <T> type of pooled instance
     * @param initialSize initial size of the pool
     * @param clazz class of pooled instance 
     * @param constructor constructor of pooled instance 
     * @return pool
     */
    static <T> Pool<T> unbounded(int initialSize, Class<T> clazz, Supplier<T> constructor) {
        if (Pooled.class.isAssignableFrom(clazz)) {
            return unbounded(initialSize, clazz, constructor, instance -> ((Pooled) instance).reset());
        }

        return new PoolImpl<>(initialSize, clazz, constructor, null);
    }

    /**
     * Returns a new unbounded pool.
     * The pool will be prefilled with up to {@code initialSize} elements.
     * 
     * @param <T> type of pooled instance
     * @param initialSize initial size of the pool
     * @param clazz class of pooled instance 
     * @param constructor constructor of pooled instance
     * @param reset consumer to reset the state of pooled instances 
     * @return pool
     */
    static <T> Pool<T> unbounded(int initialSize, Class<T> clazz, Supplier<T> constructor, Consumer<T> reset) {
        return new PoolImpl<>(initialSize, clazz, constructor, reset);
    }

    /**
     * Retrieves an instance from the pool.
     * After the instance is no longer needed, it should be returned to the pool using {@link #free(Object)}.
     * 
     * @return pooled instance 
     */
    T getInstance();

    /**
     * Passes an instance from the pool to the consumer.
     * The instance will be automatically returned to the pool after the consumer has finished.
     * 
     * <p>
     * Do not store a reference to the pooled instance outside of the consumer body.
     * Do not pass the pooled instance to {@link #free(Object)} within the consumer body.
     * 
     * @param consumer code accepting an instance
     */
    default void withInstanceNoResult(@NonNull Consumer<T> consumer) {
        T instance = getInstance();
        try {
            consumer.accept(instance);
        } finally {
            free(instance);
        }
    }

    /**
     * Passes an instance from the pool to the function and returning its result.
     * The instance will be automatically returned to the pool after the function has finished.
     * 
     * <p>
     * Do not store a reference to the pooled instance outside of the function body.
     * Do not pass the pooled instance to {@link #free(Object)} within the function body. 
     * 
     * @param <R> type of result
     * @param function code accepting an instance and returning the result
     * @return result of the function
     */
    @NullUnmarked
    default <R> R withInstance(Function<T, R> function) {
        T instance = getInstance();
        try {
            return function.apply(instance);
        } finally {
            free(instance);
        }
    }

    /**
     * Returns the instance to the pool to be reused.
     * 
     * @param instance obtained from {@link #getInstance()}
     */
    void free(T instance);

}

final class PoolImpl<T> implements Pool<T> {

    protected final Bag<T> instances;
    private final Supplier<T> constructor;
    private final Consumer<T> reset;

    protected PoolImpl(int initialSize, Class<T> clazz, Supplier<T> constructor, Consumer<T> reset) {
        this.instances = new Bag<>(clazz, 1024);
        this.constructor = constructor;
        this.reset = reset;

        for (int i = 0; i < initialSize; i++) {
            this.instances.add(constructor.get());
        }
    }

    @Override
    public @NonNull T getInstance() {
        if (instances.size() == 0) {
            return constructor.get();
        }

        return instances.removeLast();
    }

    @Override
    public void free(@NonNull T instance) {
        if (reset != null) {
            reset.accept(instance);
        }

        this.instances.add(instance);
    }

}
