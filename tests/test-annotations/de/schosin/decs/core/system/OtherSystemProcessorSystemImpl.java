package de.schosin.decs.core.system;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class OtherSystemProcessorSystemImpl extends SystemProcessorTest.OtherSystemProcessorSystem implements SystemType {
    private final InternalWorld _world;

    public OtherSystemProcessorSystemImpl(InternalWorld world) {
        this._world = world;
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
    }

    @Override
    public final void runSystem() {
        // begin()
        begin();
    }
}
