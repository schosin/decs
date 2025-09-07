package de.schosin.decs.core.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.annotations.Delta;
import de.schosin.decs.tests.components.Position;

public class SystemProcessorTest {

    @Test
    void testSystemProcessor() {
        var world = (InternalWorld) World.builder()
                .add(OtherSystemProcessorSystem.class)
                .add(SystemProcessorSystem.class)
                .build();

        var values = world.getValues();
        values.delta = 0.1f;
        values.foo = 42f;

        var other = world.getSystem(OtherSystemProcessorSystem.class);
        var system = world.getSystem(SystemProcessorSystem.class);

        world.process();
        assertThat(system.count).isOne();
        assertThat(system.world).isSameAs(world);
        assertThat(system.delta).isEqualTo(0.1f);
        assertThat(system.delta2).isEqualTo(0.1f);
        assertThat(system.foo).isEqualTo(42f);
        assertThat(system.utility).isNotNull();

        assertThat(other.count).isOne();
    }

    public static class SystemProcessorSystem {

        private int count;

        private World world;
        private float delta;
        private float delta2;
        private float foo;
        private TestUtility utility;

        @SystemProcessor
        public final void begin(World world, @Delta float delta, @Value("delta") float delta2, @Value("foo") float foo, @Utility TestUtility utility) {
            this.count++;

            this.world = world;
            this.delta = delta;
            this.delta2 = delta2;
            this.foo = foo;
            this.utility = utility;
        }

    }

    public static class OtherSystemProcessorSystem {

        private int count;

        @SystemProcessor
        public final void begin() {
            count++;
        }

    }

    public abstract static class TestUtility {

        @Archetype
        abstract void create(int count);

        @EntityInitializer
        final void create(int index, Position pos) {
        }

    }

}
