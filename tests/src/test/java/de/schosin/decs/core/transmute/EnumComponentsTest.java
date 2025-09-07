package de.schosin.decs.core.transmute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Faction;
import de.schosin.decs.tests.components.Player;
import de.schosin.decs.tests.components.Position;

public class EnumComponentsTest {

    @Test
    void testSystem() {
        var world = (InternalWorld) World.builder()
                .add(EnumComponentsSystem.class)
                .build();

        var utility = world.getUtility(EnumComponentsUtility.class);
        utility.create(3);

        var system = world.getSystem(EnumComponentsSystem.class);

        world.process();
        assertThat(system.add).isEqualTo(3);
        assertThat(system.all).isEqualTo(3);
        assertThat(system.player).isEqualTo(1);
        assertThat(system.nonPlayer).isEqualTo(2);
    }

    public static class EnumComponentsSystem {

        private int add;
        private int all;
        private int player;
        private int nonPlayer;

        @All
        @EntityProcessor
        public final void add(Entity entity, @Utility EnumComponentsUtility utility) {
            switch (entity.id() % 3) {
                case 0 -> utility.addPlayer(entity, Player.PLAYER, Faction.FRIENDLY);
                case 1 -> utility.addFaction(entity, Faction.NEUTRAL);
                default -> utility.addFaction(entity, Faction.HOSTILE);
            }

            this.add++;
        }

        @All
        @EntityProcessor
        public final void all(int entityId, Player player, Faction faction, Position pos) {
            if (player != null) {
                assertThat(player).isSameAs(switch (faction) {
                    case FRIENDLY -> Player.PLAYER;
                    case NEUTRAL -> null;
                    case HOSTILE -> null;
                });
            } else {
                assertThat(faction).isNotSameAs(Faction.FRIENDLY);
            }

            assertThat(faction).isNotNull();
            assertThat(pos).isNotNull();

            this.all++;
        }

        @All(Player.class)
        @EntityProcessor
        public final void player(int entityId, Player player, Faction faction, Position pos) {
            assertThat(player).isNotNull();
            assertThat(faction).isNotNull();
            assertThat(pos).isNotNull();

            this.player++;
        }

        @None(Player.class)
        @EntityProcessor
        public final void nonPlayer(int entityId, Faction faction, Position pos) {
            assertThat(faction).isNotNull();
            assertThat(pos).isNotNull();

            this.nonPlayer++;
        }

    }

    public abstract static class EnumComponentsUtility {

        @Archetype
        abstract void create(int count);

        @EntityInitializer
        final void create(int index, Position pos) {
        }

        @Transmute
        abstract void addPlayer(Entity entity, Player player, Faction faction);

        @EntityInitializer
        final void addPlayer(int entityId, Player player, Faction faction) {
            // TODO this should be optional when no mutable components present
            var todo = true;
        }

        @Transmute
        abstract void addFaction(Entity entity, Faction faction);

        @EntityInitializer
        final void addFaction(int entityId, Faction faction) {
            // TODO this should be optional when no mutable components present
            var todo = true;
        }

    }

}
