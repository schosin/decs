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
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class TransmuteTest {

    @Test
    void testTransmuteInEntityProcessor() {
        var world = (InternalWorld) World.builder()
                .add(TransmuteTestSystem.class)
                .build();

        var archetype = world.getUtility(TestTransmute.class);
        var system = world.getSystem(TransmuteTestSystem.class);

        archetype.create(1);
        assertThat(system.posCount).isZero();
        assertThat(system.bothCount).isZero();

        world.process();
        assertThat(system.posCount).isOne();
        assertThat(system.bothCount).isOne(); 
    }

    public static class TransmuteTestSystem {

        private int posCount;
        private int bothCount;

        @All(Position.class)
        @None(Velocity.class)
        @EntityProcessor
        public final void process(Entity entity, Position pos, @Utility TestTransmute transmute) {
            assertThat(pos).isNotNull();
            posCount++;

            transmute.add(entity);
        }

        @All({ Position.class, Velocity.class })
        @EntityProcessor
        public final void process(int id, Position pos, Velocity velocity) {
            assertThat(pos).isNotNull();
            assertThat(velocity).isNotNull();

            bothCount++;
        }

    }

    public abstract static class TestTransmute {

        @Archetype
        abstract void create(int count);

        @EntityInitializer
        final void create(int index, Position pos) {
        }

        @Transmute
        abstract void add(Entity entity);

        @EntityInitializer
        final void add(int entityId, Velocity velocity) {
        }

    }

}
