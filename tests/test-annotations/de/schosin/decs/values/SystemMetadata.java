package de.schosin.decs.values;

import de.schosin.decs.api.annotations.Generated;
import java.util.Set;
import java.util.function.Function;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class SystemMetadata {
    private final Class<?> clazz;

    private final Class<?> implementation;

    private final Function<Object, Object> constructor;

    private final Set<Class<?>> dependencies;

    SystemMetadata(Class<?> clazz, Class<?> implementation, Function<Object, Object> constructor,
            Set<Class<?>> dependencies) {
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
