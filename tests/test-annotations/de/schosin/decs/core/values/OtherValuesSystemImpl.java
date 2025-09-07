package de.schosin.decs.core.values;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.values.Values;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class OtherValuesSystemImpl extends ValuesTest2.OtherValuesSystem implements SystemType {
    private final InternalWorld _world;

    private final Values _values;

    public OtherValuesSystemImpl(InternalWorld world) {
        this._world = world;
        this._values = world.getValues();
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
    }

    @Override
    public final void runSystem() {
        // sysA(float foo, List)
        sysA(_values.aaa, _values.bbb);
    }
}
