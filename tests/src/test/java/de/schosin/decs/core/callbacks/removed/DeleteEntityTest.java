package de.schosin.decs.core.callbacks.removed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class DeleteEntityTest {

    @Test
    void testCreateEntity() {
        var world = (InternalWorld) World.builder()
                .add(DeleteEntityTestSystem.class)
                .build();

        var system = world.getSystem(DeleteEntityTestSystem.class);
        assertThat(system.order).isEmpty();

        system.createEntity(1);
        assertThat(system.order).containsExactly("createEntity");

        world.process();
        assertThat(system.order).containsExactly(
                "createEntity",
                "process",
                "removedAll",
                "removedPos",
                "removedPosOnly");
    }

    public static abstract class DeleteEntityTestSystem {

        private final List<String> order = new ArrayList<>();

        @All
        @EntityProcessor
        public final void process(Entity entity) {
            entity.delete();
            order.add("process");
        }

        @All
        @Removed
        public final void removedAll(int entityId) {
            order.add("removedAll");
        }

        @All(Position.class)
        @Removed
        public final void removedPos(int entityId) {
            order.add("removedPos");
        }

        @All(Position.class)
        @None(Velocity.class)
        @Removed
        public final void removedPosOnly(int entityId) {
            order.add("removedPosOnly");
        }

        @All(Velocity.class)
        @Removed
        public final void removedVelocity(int entityId) {
            order.add("removedVelocity");
        }

        @Archetype
        abstract void createEntity(int count);

        @EntityInitializer
        final void createEntity(int index, Position pos) {
            order.add("createEntity");
        }

    }

}
