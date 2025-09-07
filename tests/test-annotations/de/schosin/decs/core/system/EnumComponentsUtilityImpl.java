package de.schosin.decs.core.system;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.EntityArchetype;
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

    private final CreateArchetype2 createArchetype2;

    public EnumComponentsUtilityImpl(InternalWorld world) {
        this.createArchetype = new CreateArchetype(world.getEntityArchetype(Position.class, Player.class, Faction.class));
        this.createArchetype2 = new CreateArchetype2(world.getEntityArchetype(Position.class, Faction.class));
    }

    @Override
    public final void create(int count, Player player, Faction faction) {
        this.createArchetype.apply(count, player, faction);
    }

    @Override
    public final void create(int count, Faction faction) {
        this.createArchetype2.apply(count, faction);
    }

    private final class CreateArchetype {
        private final EntityArchetype _archetype;

        private final Bag<Position> positionComponents;

        private final Bag<Faction> factionComponents;

        private CreateArchetype(EntityArchetype archetype) {
            this._archetype = archetype;
            this.positionComponents = archetype.getData(Position.class);
            this.factionComponents = archetype.getData(Faction.class);
        }

        private final void apply(int count, Player player, Faction faction) {
            int _index = this._archetype.createEntities(count);

            Position[] _pos = this.positionComponents.getData();
            Faction[] _faction = this.factionComponents.getData();

            for (int _i = _index, _s = _index + count; _i < _s; _i++) {
                _faction[_i] = faction;

                EnumComponentsUtilityImpl.this.create(_i, player, faction, _pos[_i]);
            }
        }
    }

    private final class CreateArchetype2 {
        private final EntityArchetype _archetype;

        private final Bag<Position> positionComponents;

        private final Bag<Faction> factionComponents;

        private CreateArchetype2(EntityArchetype archetype) {
            this._archetype = archetype;
            this.positionComponents = archetype.getData(Position.class);
            this.factionComponents = archetype.getData(Faction.class);
        }

        private final void apply(int count, Faction faction) {
            int _index = this._archetype.createEntities(count);

            Position[] _pos = this.positionComponents.getData();
            Faction[] _faction = this.factionComponents.getData();

            for (int _i = _index, _s = _index + count; _i < _s; _i++) {
                _faction[_i] = faction;

                EnumComponentsUtilityImpl.this.create(_i, faction, _pos[_i]);
            }
        }
    }
}
