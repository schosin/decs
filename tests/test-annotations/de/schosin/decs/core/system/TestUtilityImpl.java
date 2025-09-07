package de.schosin.decs.core.system;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.UtilityType;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.tests.components.Position;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class TestUtilityImpl extends SystemProcessorTest.TestUtility implements UtilityType {
    private final CreateArchetype createArchetype;

    public TestUtilityImpl(InternalWorld world) {
        this.createArchetype = new CreateArchetype(world.getEntityArchetype(Position.class));
    }

    @Override
    public final void create(int count) {
        this.createArchetype.apply(count);
    }

    private final class CreateArchetype {
        private final EntityArchetype _archetype;

        private final Bag<Position> positionComponents;

        private CreateArchetype(EntityArchetype archetype) {
            this._archetype = archetype;
            this.positionComponents = archetype.getData(Position.class);
        }

        private final void apply(int count) {
            int _index = this._archetype.createEntities(count);

            Position[] _pos = this.positionComponents.getData();

            for (int _i = _index, _s = _index + count; _i < _s; _i++) {
                TestUtilityImpl.this.create(_i, _pos[_i]);
            }
        }
    }
}
