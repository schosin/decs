package de.schosin.decs.core.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
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
        utility.create(1, Player.PLAYER, Faction.FRIENDLY);
        utility.create(2, Faction.NEUTRAL);
        utility.create(4, Faction.HOSTILE);

        var system = world.getSystem(EnumComponentsSystem.class);

        world.process();
        assertThat(system.all).isEqualTo(7);
        assertThat(system.player).isEqualTo(1);
        assertThat(system.nonPlayer).isEqualTo(6);
    }

    public static class EnumComponentsSystem {

        private int all;
        private int player;
        private int nonPlayer;

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
        abstract void create(int count, Player player, Faction faction);

        @EntityInitializer
        final void create(int index, Player player, Faction faction, Position pos) {
        }

        @Archetype
        abstract void create(int count, Faction faction);

        @EntityInitializer
        final void create(int index, Faction faction, Position pos) {
        }

    }

}
