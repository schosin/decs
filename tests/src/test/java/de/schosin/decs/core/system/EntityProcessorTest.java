package de.schosin.decs.core.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.archetypes.PhysicsArchetype;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class EntityProcessorTest {

    @Test
    void testSystemProcessor() {
        var world = (InternalWorld) World.builder()
                .add(EntityProcessorSystem.class)
                .build();

        var archetype = world.getUtility(PhysicsArchetype.class);
        var system = world.getSystem(EntityProcessorSystem.class);

        archetype.create(1);
        assertThat(system.count).isZero();

        world.process();
        assertThat(system.count).isOne();
    }

    public static class EntityProcessorSystem {

        private int count;

        @All(Position.class)
        @EntityProcessor
        public final void process(int id, Entity entity, Position pos, Velocity velocity) {
            assertThat(id).isEqualTo(entity.id());
            assertThat(pos).isNotNull();
            assertThat(velocity).isNotNull();

            this.count++;
        }

    }

}
