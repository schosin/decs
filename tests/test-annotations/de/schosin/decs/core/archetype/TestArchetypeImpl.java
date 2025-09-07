package de.schosin.decs.core.archetype;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.UtilityType;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class TestArchetypeImpl extends ArchetypeTest.TestArchetype implements UtilityType {
    private final CreateEntityArchetype createEntityArchetype;

    public TestArchetypeImpl(InternalWorld world) {
        this.createEntityArchetype = new CreateEntityArchetype(world.getEntityArchetype(Position.class, Velocity.class));
    }

    @Override
    public final void createEntity(int count) {
        this.createEntityArchetype.apply(count);
    }

    private final class CreateEntityArchetype {
        private final EntityArchetype _archetype;

        private final Bag<Position> positionComponents;

        private final Bag<Velocity> velocityComponents;

        private CreateEntityArchetype(EntityArchetype archetype) {
            this._archetype = archetype;
            this.positionComponents = archetype.getData(Position.class);
            this.velocityComponents = archetype.getData(Velocity.class);
        }

        private final void apply(int count) {
            int _index = this._archetype.createEntities(count);

            Position[] _position = this.positionComponents.getData();
            Velocity[] _velocity = this.velocityComponents.getData();

            for (int _i = _index, _s = _index + count; _i < _s; _i++) {
                TestArchetypeImpl.this.createEntity(_i, _position[_i], _velocity[_i]);
            }
        }
    }
}
