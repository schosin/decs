package de.schosin.decs.core.builder;

import de.schosin.decs.api.World;
import de.schosin.decs.api.builder.WorldBuilder;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.core.CoreWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractCoreWorldBuilder<SELF extends AbstractCoreWorldBuilder<SELF>> implements WorldBuilder {

    private final Map<Class<?>, Object> singletons = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public final SELF singleton(Object singleton) {
        return (SELF) this;
    }

    protected final boolean isSingletonRegistered(Class<?> dependency) {
        return this.singletons.containsKey(dependency);
    }

    protected abstract Function<InternalWorld, SystemInvocation> getSystemInvocation();

    @Override
    public final World build() {
        Function<InternalWorld, SystemInvocation> systemInvocation = getSystemInvocation();

        return new CoreWorld(systemInvocation, singletons);
    }

}
