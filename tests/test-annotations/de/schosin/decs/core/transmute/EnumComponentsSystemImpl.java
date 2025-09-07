package de.schosin.decs.core.transmute;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalEntity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.tests.components.Faction;
import de.schosin.decs.tests.components.Position;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class EnumComponentsSystemImpl extends EnumComponentsTest.EnumComponentsSystem implements SystemType {
    private final InternalWorld _world;

    private final EnumComponentsTest.EnumComponentsUtility enumComponentsUtility;

    private final Bag<Add> add;

    private final Bag<All> all;

    private final Bag<Player> player;

    private final Bag<NonPlayer> nonPlayer;

    public EnumComponentsSystemImpl(InternalWorld world) {
        this._world = world;
        this.enumComponentsUtility = world.getUtility(EnumComponentsTest.EnumComponentsUtility.class);
        this.add = new Bag<>(Add.class, 4);
        this.all = new Bag<>(All.class, 4);
        this.player = new Bag<>(Player.class, 4);
        this.nonPlayer = new Bag<>(NonPlayer.class, 4);
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
        if (Add.COMPOSITION.matches(archetype)) {
            this.add.add(new Add(this._world, archetype, this));
        }
        if (All.COMPOSITION.matches(archetype)) {
            this.all.add(new All(this._world, archetype, this));
        }
        if (Player.COMPOSITION.matches(archetype)) {
            this.player.add(new Player(this._world, archetype, this));
        }
        if (NonPlayer.COMPOSITION.matches(archetype)) {
            this.nonPlayer.add(new NonPlayer(this._world, archetype, this));
        }
    }

    @Override
    public final void runSystem() {
        // add(Entity, EnumComponentsUtility)
        Add[] addData = this.add.getData();
        for (int i = 0, s = this.add.size(); i<s; i++) {
            addData[i].run();
        }

        // Flush potential entity changes by "this.add"
        this._world.flushChanges();

        // all(int entityId, Player, Faction, Position)
        All[] allData = this.all.getData();
        for (int i = 0, s = this.all.size(); i<s; i++) {
            allData[i].run();
        }

        // player(int entityId, Player, Faction, Position)
        Player[] playerData = this.player.getData();
        for (int i = 0, s = this.player.size(); i<s; i++) {
            playerData[i].run();
        }

        // nonPlayer(int entityId, Player, Faction, Position)
        NonPlayer[] nonPlayerData = this.nonPlayer.getData();
        for (int i = 0, s = this.nonPlayer.size(); i<s; i++) {
            nonPlayerData[i].run();
        }
    }

    private static final class Add implements InternalEntity {
        private static final Composition COMPOSITION = Composition.all().build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final EnumComponentsSystemImpl _system;

        private final EnumComponentsTest.EnumComponentsUtility enumComponentsUtility;

        private int _index = -1;

        private int _entityId = -1;

        private Add(World world, EntityArchetype archetype, EnumComponentsSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this.enumComponentsUtility = world.getUtility(EnumComponentsTest.EnumComponentsUtility.class);
        }

        private final void run() {
            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._index = _i;
                this._entityId = _data[_i];

                this._system.add(this, enumComponentsUtility);
            }

            // Reset state to avoid improper use as Entity outside of the loop
            this._index = -1;
            this._entityId = -1;
        }

        @Override
        public final int id() {
            return this._entityId;
        }

        @Override
        public final void delete() {
            this._archetype.deleteEntity(this._index);
        }

        @Override
        public final int index() {
            return this._index;
        }

        @Override
        public final EntityArchetype archetype() {
            return this._archetype;
        }

        @Override
        public final String toString() {
            return new StringBuilder().append("Entity(id = ").append(this._entityId).append(")").toString();
        }
    }

    private static final class All {
        private static final Composition COMPOSITION = Composition.all().build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final EnumComponentsSystemImpl _system;

        private final de.schosin.decs.tests.components.Player playerComponents;

        private final Bag<Faction> factionComponents;

        private final Bag<Position> positionComponents;

        private All(World world, EntityArchetype archetype, EnumComponentsSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this.playerComponents = archetype.getComponents().contains(de.schosin.decs.tests.components.Player.class) ? de.schosin.decs.tests.components.Player.PLAYER : null;
            this.factionComponents = archetype.getData(Faction.class);
            this.positionComponents = archetype.getData(Position.class);
        }

        private final void run() {
            Faction[] faction = this.factionComponents != null ? this.factionComponents.getData() : null;
            Position[] pos = this.positionComponents != null ? this.positionComponents.getData() : null;

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._system.all(_data[_i], this.playerComponents, faction != null ? faction[_i] : null, pos != null ? pos[_i] : null);
            }
        }
    }

    private static final class Player {
        private static final Composition COMPOSITION = Composition
            .all(de.schosin.decs.tests.components.Player.class)
            .build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final EnumComponentsSystemImpl _system;

        private final Bag<Faction> factionComponents;

        private final Bag<Position> positionComponents;

        private Player(World world, EntityArchetype archetype, EnumComponentsSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this.factionComponents = archetype.getData(Faction.class);
            this.positionComponents = archetype.getData(Position.class);
        }

        private final void run() {
            Faction[] faction = this.factionComponents != null ? this.factionComponents.getData() : null;
            Position[] pos = this.positionComponents != null ? this.positionComponents.getData() : null;

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._system.player(_data[_i], de.schosin.decs.tests.components.Player.PLAYER, faction != null ? faction[_i] : null, pos != null ? pos[_i] : null);
            }
        }
    }

    private static final class NonPlayer {
        private static final Composition COMPOSITION = Composition
            .none(de.schosin.decs.tests.components.Player.class)
            .build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final EnumComponentsSystemImpl _system;

        private final Bag<Faction> factionComponents;

        private final Bag<Position> positionComponents;

        private NonPlayer(World world, EntityArchetype archetype, EnumComponentsSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this.factionComponents = archetype.getData(Faction.class);
            this.positionComponents = archetype.getData(Position.class);
        }

        private final void run() {
            Faction[] faction = this.factionComponents != null ? this.factionComponents.getData() : null;
            Position[] pos = this.positionComponents != null ? this.positionComponents.getData() : null;

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._system.nonPlayer(_data[_i], null, faction != null ? faction[_i] : null, pos != null ? pos[_i] : null);
            }
        }
    }
}
