package de.schosin.decs.core.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.One;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Faction;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.PositionInterface;

public class InterfaceComponentProcessorTest {

    @Test
    void testEntityProcessor() {
        var world = (InternalWorld) World.builder()
                .add(InterfaceComponentProcessorTestSystem.class)
                .build();

        var system = world.getSystem(InterfaceComponentProcessorTestSystem.class);
        system.createClass(1, 10f, 11f, Faction.FRIENDLY);
        system.createInterface(1, 20, 21, Faction.NEUTRAL);
        system.createBoth(1, 30f, 31f, 32, 33, Faction.HOSTILE);

        world.process();
        assertThat(system.classCount).isEqualTo(1);
        assertThat(system.interfaceCount).isEqualTo(1);
        assertThat(system.bothCount).isEqualTo(1);
        
        // TODO add test for @Inserted and @Removed
        // TODO add test for transmute (add interface)
        // TODO add test for EntityRef
    }

    public static abstract class InterfaceComponentProcessorTestSystem {

        private int classCount;
        private int interfaceCount;
        private int bothCount;

        @All(Faction.class)
        @One({ Position.class, PositionInterface.class })
        @EntityProcessor
        public final void process(Faction type, Position pos, PositionInterface pos2) {
            assertThat(type).isNotNull();

            switch (type) {
                case FRIENDLY -> {
                    assertThat(pos).isNotNull();
                    assertThat(pos.x).isEqualTo(10f);
                    assertThat(pos.y).isEqualTo(11f);

                    this.classCount++;
                }
                case NEUTRAL -> {
                    assertThat(pos2).isNotNull();
                    assertThat(pos2.x()).isEqualTo(20);
                    assertThat(pos2.y()).isEqualTo(21);

                    this.interfaceCount++;
                }
                case HOSTILE -> {
                    assertThat(pos).isNotNull();
                    assertThat(pos.x).isEqualTo(30f);
                    assertThat(pos.y).isEqualTo(31f);

                    assertThat(pos2).isNotNull();
                    assertThat(pos2.x()).isEqualTo(32);
                    assertThat(pos2.y()).isEqualTo(33);

                    this.bothCount++;
                }
                default -> fail("Broken test setup, enum value not handled: " + type);
            }
        }

        @Archetype
        abstract void createClass(int count, float x, float y, Faction type);

        @EntityInitializer
        final void createClass(int index, float x, float y, Faction type, Position pos) {
            assertThat(type).as("proper test setup").isEqualTo(Faction.FRIENDLY);

            pos.x = x;
            pos.y = y;
        }

        @Archetype
        abstract void createInterface(int count, int x, int y, Faction type);

        @EntityInitializer
        final void createInterface(int index, int x, int y, Faction type, PositionInterface pos) {
            assertThat(type).as("proper test setup").isEqualTo(Faction.NEUTRAL);

            pos.x(x);
            pos.y(y);
        }

        @Archetype
        abstract void createBoth(int count, float x, float y, int x2, int y2, Faction type);

        @EntityInitializer
        final void createBoth(int index, float x, float y, int x2, int y2, Faction type, Position pos, PositionInterface pos2) {
            assertThat(type).as("proper test setup").isEqualTo(Faction.HOSTILE);

            pos.x = x;
            pos.y = y;

            pos2.x(x2);
            pos2.y(y2);
        }

    }

}
