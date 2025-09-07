package de.schosin.decs.core.system;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.values.Values;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class SystemProcessorSystemImpl extends SystemProcessorTest.SystemProcessorSystem implements SystemType {
    private final InternalWorld _world;

    private final Values _values;

    private final SystemProcessorTest.TestUtility testUtility;

    public SystemProcessorSystemImpl(InternalWorld world) {
        this._world = world;
        this._values = world.getValues();
        this.testUtility = world.getUtility(SystemProcessorTest.TestUtility.class);
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
    }

    @Override
    public final void runSystem() {
        // begin(World, float delta, float delta2, float foo, TestUtility)
        begin(_world, _values.delta, _values.delta, _values.foo, testUtility);
    }
}
