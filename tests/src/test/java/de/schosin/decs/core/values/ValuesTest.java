package de.schosin.decs.core.values;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.archetypes.PhysicsArchetype;
import de.schosin.decs.tests.components.Velocity;

public class ValuesTest {

    @Test
    void testValues() {
        var world = (InternalWorld) World.builder()
                .add(OtherValuesSystem.class) 
                .add(ValuesSystem.class)
                .build();

        var archetype = world.getUtility(PhysicsArchetype.class);
        archetype.create(1);

        var values = world.getValues();
        values.foo = 42f;
        values.bar = List.of("1", "2");

        world.process();

        var system = world.getSystem(ValuesSystem.class);
        assertThat(system.foo).isEqualTo(42f);
        assertThat(system.bar).isSameAs(values.bar);
    }

    public static class ValuesSystem {

        private float foo = -1f;
        private List<String> bar = null;

        @SystemProcessor
        final void sysA(@Value("foo") float foo, @Bar List<String> bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @All
        @EntityProcessor
        final void processA(int entityId, @Value("foo") float foo, @Bar List<String> bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @All(Velocity.class)
        @EntityProcessor
        final void processB(int entityId, @Value("foo") float foo, @Bar List<String> bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @SystemProcessor
        final void sysB(@Value("foo") float foo, @Bar List<String> bar) {
            this.foo = foo;
            this.bar = bar;
        }

    }

    public static class OtherValuesSystem {

        @SystemProcessor
        final void sysA(@Value("foo") float foo, @Bar List<String> bar) {
        }

    }

    @Target(ElementType.PARAMETER)
    @Value("bar")
    @interface Bar {
    }

}
