package de.schosin.decs.values;

import java.util.Comparator;

public final class ComponentMetadata<T> {
    public static final Comparator<ComponentMetadata> COMPARATOR = Comparator.comparingInt(ComponentMetadata::getId);

    private final int id;

    private final Class<T> type;

    Object pool;

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
