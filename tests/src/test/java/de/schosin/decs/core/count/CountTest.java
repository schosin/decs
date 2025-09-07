package de.schosin.decs.core.count;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.Count;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class CountTest {

    @Test
    void testCount() {
        var world = (InternalWorld) World.builder()
                .add(CountTestSystem.class)
                .build();

        var utility = world.getUtility(CountTestUtility.class);
        assertThat(utility.all()).as("utility all").isEqualTo(0);
        assertThat(utility.pos()).as("utility pos").isEqualTo(0);

        var system = world.getSystem(CountTestSystem.class);
        assertThat(system.all()).as("system all").isEqualTo(0);
        assertThat(system.pos()).as("system pos").isEqualTo(0);

        system.createPos(1);
        system.createVelocity(2);
        assertThat(utility.all()).as("utility all").isEqualTo(0);
        assertThat(utility.pos()).as("utility pos").isEqualTo(0);

        assertThat(system.all()).as("system all").isEqualTo(0);
        assertThat(system.pos()).as("system pos").isEqualTo(0);

        world.process();
        assertThat(utility.all()).as("utility all").isEqualTo(3);
        assertThat(utility.pos()).as("utility pos").isEqualTo(1);

        assertThat(system.all()).as("system all").isEqualTo(3);
        assertThat(system.pos()).as("system pos").isEqualTo(1);
    }

    public static abstract class CountTestUtility {

        @All
        @Count
        public abstract int all();

        @All(Position.class)
        @Count
        public abstract int pos();

    }

    public static abstract class CountTestSystem {

        @SystemProcessor
        public final void system() {
        }

        @All
        @Count
        public abstract int all();

        @All(Position.class)
        @Count
        public abstract int pos();

        @Archetype
        abstract void createPos(int count);

        @EntityInitializer
        final void createPos(int index, Position pos) {
        }

        @Archetype
        abstract void createVelocity(int count);

        @EntityInitializer
        final void createVelocity(int index, Velocity velocity) {
        }

    }

}
