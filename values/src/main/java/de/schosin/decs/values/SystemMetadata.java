package de.schosin.decs.values;

import java.util.Set;
import java.util.function.Function;

public final class SystemMetadata {

    private final Class<?> clazz;
    private final Class<?> implementation;
    private final Function<Object, Object> constructor;
    private final Set<Class<?>> dependencies;

    public SystemMetadata(Class<?> clazz, Class<?> implementation, Function<Object, Object> constructor, Set<Class<?>> dependencies) {
        this.clazz = clazz;
        this.implementation = implementation;
        this.constructor = constructor;
        this.dependencies = dependencies;
    }

    public Class<?> clazz() {
        return this.clazz;
    }

    public Class<?> implementation() {
        return this.implementation;
    }

    public Function<Object, Object> constructor() {
        return this.constructor;
    }

    public Set<Class<?>> dependencies() {
        return this.dependencies;
    }

}
