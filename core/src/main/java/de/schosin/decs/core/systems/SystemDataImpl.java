package de.schosin.decs.core.systems;

import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.values.SystemMetadata;

public final class SystemDataImpl implements SystemData {

    private final SystemMetadata metadata;

    public SystemDataImpl(SystemMetadata metadata) {
        this.metadata = metadata;
    }

    public SystemMetadata metadata() {
        return this.metadata;
    }

    @Override
    public SystemType getInstance(InternalWorld world) {
        return (SystemType) metadata.constructor().apply(world);
    }

}
