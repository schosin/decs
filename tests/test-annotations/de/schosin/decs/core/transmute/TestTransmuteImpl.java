package de.schosin.decs.core.transmute;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.Transition;
import de.schosin.decs.api.entities.Transmutation;
import de.schosin.decs.api.internal.InternalEntity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.UtilityType;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class TestTransmuteImpl extends TransmuteTest.TestTransmute implements UtilityType {
    private final CreateArchetype createArchetype;

    private final Bag<AddTransmuter> addTransmuter = new Bag(AddTransmuter.class, 4);

    public TestTransmuteImpl(InternalWorld world) {
        this.createArchetype = new CreateArchetype(world.getEntityArchetype(Position.class));
    }

    @Override
    public final void create(int count) {
        this.createArchetype.apply(count);
    }

    @Override
    public final void add(Entity __entity) {
        InternalEntity _entity = (InternalEntity) __entity;
        int _index = _entity.index();
        EntityArchetype _archetype = _entity.archetype();

        // Retrieve target archetype, return early if null (entity marked for deletion)
        Transition _transition = _archetype.moveEntity(_index, AddTransmuter.TRANSMUTATION);
        if (_transition == null) {
            return;
        }

        // Retrieve transmuter, create new if not yet used
        EntityArchetype _targetArchetype = _transition.archetype();
        AddTransmuter _transmuter = this.addTransmuter.get(_targetArchetype.id());
        if (_transmuter == null) {
            _transmuter = new AddTransmuter(_archetype, _targetArchetype);
            this.addTransmuter.set(_targetArchetype.id(), _transmuter);
        }

        // Apply transmuter
        _transmuter.apply(_transition.index(), _entity.id());

        _transition.free();
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
                TestTransmuteImpl.this.create(_i, _pos[_i]);
            }
        }
    }

    private final class AddTransmuter {
        private static final Transmutation TRANSMUTATION = Transmutation.builder()
            .add(Velocity.class)
            .build();

        private final Bag<Velocity> velocityComponents;

        private AddTransmuter(EntityArchetype archetype, EntityArchetype targetArchetype) {
            this.velocityComponents = targetArchetype.getData(Velocity.class);
        }

        private final void apply(int _index, int _entityId) {
            // TODO set velocity

            TestTransmuteImpl.this.add(_entityId, this.velocityComponents.getUnsafe(_index));
        }
    }
}
