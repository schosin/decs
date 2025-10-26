package de.schosin.decs.core.builder;

import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.values.Types;

import java.util.function.Function;

@SuppressWarnings("unused") // used reflectively in api module
public final class CoreWorldBuilder extends AbstractCoreWorldBuilder<CoreWorldBuilder> {

    private final Class<? extends SystemInvocation> systemInvocation;

    public CoreWorldBuilder(Class<? extends SystemInvocation> systemInvocation) {
        this.systemInvocation = systemInvocation;
    }

    @Override
    protected Function<InternalWorld, SystemInvocation> getSystemInvocation() {
        return world -> (SystemInvocation) Types.getSystemInvocation(systemInvocation, world);
    }

}
