package de.schosin.decs.core.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.exceptions.InvalidUtilityException;
import de.schosin.decs.core.CoreWorld;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class ArchetypeTest {

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 5 })
    void testArchetypeInterface(int count) {
        var world = (CoreWorld) World.builder().add(ArchetypeTestSystem.class).build();

        var archetype = world.getUtility(TestArchetype.class);
        assertThat(archetype).as("invocation.createArchetype must return an instance").isNotNull();
        assertThat(archetype.count).isZero();

        var entityArchetype = world.getEntityArchetype(Position.class, Velocity.class);
        assertThat(entityArchetype.getAlive()).isZero();

        archetype.createEntity(count);
        assertThat(archetype.count).isEqualTo(count);
        assertThat(entityArchetype.getAlive()).isZero();
        
        world.process();
        assertThat(archetype.count).isEqualTo(count);
        assertThat(entityArchetype.getAlive()).isEqualTo(count);
    }

    @Test
    void testInvalidArchetype_NoArchetypeMethods() {
        var world = (CoreWorld) World.builder().add(ArchetypeTestSystem.class).build();

        assertThatThrownBy(() -> world.getUtility(NonArchetype.class))
                .isInstanceOf(InvalidUtilityException.class);
    }

    public static abstract class TestArchetype {

        private int count;

        @Archetype
        abstract void createEntity(int count);

        @EntityInitializer
        final void createEntity(int index, Position position, Velocity velocity) {
            this.count++;
        }

    }

    public abstract class NonArchetype {

        abstract int createEntity(Position position, Velocity velocity);

    }

    public static class ArchetypeTestSystem {

        @SystemProcessor
        public final void process() {
        }

    }

}
