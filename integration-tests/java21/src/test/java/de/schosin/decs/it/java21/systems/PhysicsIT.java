package de.schosin.decs.it.java21.systems;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.it.common.AbstractIntegrationTest;
import de.schosin.decs.it.java8.components.Acceleration;
import de.schosin.decs.it.java8.components.Position;
import de.schosin.decs.it.java8.components.Velocity;

public class PhysicsIT extends AbstractIntegrationTest {

    @Test
    void testSystem() {
        // Setup
        var world = (InternalWorld) World.builder()
                .add(TestSystem.class)
                .build();

        var archetype = world.getUtility(TestArchetype.class);
        archetype.createMoving(1);
        archetype.createAccelerated(2);
        archetype.createPositioned(4);

        // Call
        runSimulation(world);

        // Verify
        var system = world.getSystem(TestSystem.class);
        assertThat(system.inserted).hasSize(3);
        assertThat(system.processed).hasSize(3);
        assertThat(system.archetypeProcessed).hasSize(3);
    }

    public static class TestSystem {

        private final Set<Integer> inserted = new HashSet<>();
        private final Set<Integer> processed = new HashSet<>();
        private final Set<Integer> archetypeProcessed = new HashSet<>();

        @Inserted
        @All({ Position.class, Velocity.class })
        public final void inserted(int entityId) {
            inserted.add(entityId);
        }

        @EntityProcessor
        @All({ Position.class, Velocity.class })
        public final void process(int entityId) {
            processed.add(entityId);
        }

        @EntityProcessor
        @All({ Position.class, Velocity.class })
        public final void process(int entityId, @Utility TestArchetype archetype) {
            assertThat(archetype).isNotNull();

            archetypeProcessed.add(entityId);
        }

    }

    public abstract static class TestArchetype {

        @Archetype
        abstract void createPositioned(int count);

        @EntityInitializer
        protected final void createPositioned(int index, Position pos) {
        }

        @Archetype
        abstract void createMoving(int count);

        @EntityInitializer
        protected final void createMoving(int index, Position pos, Velocity vel) {
        }

        @Archetype
        abstract void createAccelerated(int count);

        @EntityInitializer
        protected final void createAccelerated(int index, Position pos, Velocity vel, Acceleration accel) {
        }

    }

}

record Data(int value) {
}
