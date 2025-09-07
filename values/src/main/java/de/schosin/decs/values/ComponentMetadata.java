package de.schosin.decs.values;

import java.util.Comparator;

/**
 * <b>INTERNAL API:</b> This implementation is not intended to be used.
 * 
 * Holds metadata for a component type detected during code generation.
 * {@link #getPool()} will return a {@code Bag<T>} for its pool, but its return type must be {@link Object} to avoid a cyclic dependency between this and the api module.
 * 
 * @param <T> type of component
 */
public final class ComponentMetadata<T> {

    public static final Comparator<ComponentMetadata<?>> COMPARATOR = Comparator.comparingInt(ComponentMetadata::getId);

    private final int id;
    private final Class<T> type;
    private final Object pool;

    ComponentMetadata(int id, Class<T> type, Object pool) {
        this.id = id;
        this.type = type;

        this.pool = pool;
    }

    public final int getId() {
        return this.id;
    }

    public final Class<T> getType() {
        return this.type;
    }

    public final Object getPool() {
        return this.pool;
    }

}
