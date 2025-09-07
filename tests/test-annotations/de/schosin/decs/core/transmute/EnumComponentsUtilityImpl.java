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
import de.schosin.decs.tests.components.Faction;
import de.schosin.decs.tests.components.Player;
import de.schosin.decs.tests.components.Position;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class EnumComponentsUtilityImpl extends EnumComponentsTest.EnumComponentsUtility implements UtilityType {
    private final CreateArchetype createArchetype;

    private final Bag<AddFactionTransmuter> addFactionTransmuter = new Bag(AddFactionTransmuter.class, 4);

    private final Bag<AddPlayerTransmuter> addPlayerTransmuter = new Bag(AddPlayerTransmuter.class, 4);

    public EnumComponentsUtilityImpl(InternalWorld world) {
        this.createArchetype = new CreateArchetype(world.getEntityArchetype(Position.class));
    }

    @Override
    public final void create(int count) {
        this.createArchetype.apply(count);
    }

    @Override
    public final void addFaction(Entity __entity, Faction faction) {
        InternalEntity _entity = (InternalEntity) __entity;
        int _index = _entity.index();
        EntityArchetype _archetype = _entity.archetype();

        // Retrieve target archetype, return early if null (entity marked for deletion)
        Transition _transition = _archetype.moveEntity(_index, AddFactionTransmuter.TRANSMUTATION);
        if (_transition == null) {
            return;
        }

        // Retrieve transmuter, create new if not yet used
        EntityArchetype _targetArchetype = _transition.archetype();
        AddFactionTransmuter _transmuter = this.addFactionTransmuter.get(_targetArchetype.id());
        if (_transmuter == null) {
            _transmuter = new AddFactionTransmuter(_archetype, _targetArchetype);
            this.addFactionTransmuter.set(_targetArchetype.id(), _transmuter);
        }

        // Apply transmuter
        _transmuter.apply(_transition.index(), _entity.id(), faction);

        _transition.free();
    }

    @Override
    public final void addPlayer(Entity __entity, Player player, Faction faction) {
        InternalEntity _entity = (InternalEntity) __entity;
        int _index = _entity.index();
        EntityArchetype _archetype = _entity.archetype();

        // Retrieve target archetype, return early if null (entity marked for deletion)
        Transition _transition = _archetype.moveEntity(_index, AddPlayerTransmuter.TRANSMUTATION);
        if (_transition == null) {
            return;
        }

        // Retrieve transmuter, create new if not yet used
        EntityArchetype _targetArchetype = _transition.archetype();
        AddPlayerTransmuter _transmuter = this.addPlayerTransmuter.get(_targetArchetype.id());
        if (_transmuter == null) {
            _transmuter = new AddPlayerTransmuter(_archetype, _targetArchetype);
            this.addPlayerTransmuter.set(_targetArchetype.id(), _transmuter);
        }

        // Apply transmuter
        _transmuter.apply(_transition.index(), _entity.id(), player, faction);

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
                EnumComponentsUtilityImpl.this.create(_i, _pos[_i]);
            }
        }
    }

    private final class AddFactionTransmuter {
        private static final Transmutation TRANSMUTATION = Transmutation.builder()
            .add(Faction.class)
            .build();

        private final Bag<Faction> factionComponents;

        private AddFactionTransmuter(EntityArchetype archetype, EntityArchetype targetArchetype) {
            this.factionComponents = targetArchetype.getData(Faction.class);
        }

        private final void apply(int _index, int _entityId, Faction faction) {
            this.factionComponents.setUnsafe(_index, faction);

            EnumComponentsUtilityImpl.this.addFaction(_entityId, faction);
        }
    }

    private final class AddPlayerTransmuter {
        private static final Transmutation TRANSMUTATION = Transmutation.builder()
            .add(Player.class, Faction.class)
            .build();

        private final Bag<Faction> factionComponents;

        private AddPlayerTransmuter(EntityArchetype archetype, EntityArchetype targetArchetype) {
            this.factionComponents = targetArchetype.getData(Faction.class);
        }

        private final void apply(int _index, int _entityId, Player player, Faction faction) {
            this.factionComponents.setUnsafe(_index, faction);

            EnumComponentsUtilityImpl.this.addPlayer(_entityId, player, faction);
        }
    }
}
