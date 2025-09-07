package de.schosin.decs.core.callbacks.inserted;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class CreateEntityTest {

    @Test
    void testCreateEntity() {
        var world = (InternalWorld) World.builder()
                .add(CreateEntityTestSystem.class)
                .build();

        var system = world.getSystem(CreateEntityTestSystem.class);
        assertThat(system.order).isEmpty();

        system.createEntity(1);
        assertThat(system.order).containsExactly("createEntity");

        world.process();
        assertThat(system.order).containsExactly(
                "createEntity",
                "insertedAll",
                "insertedPos",
                "insertedPosOnly");
    }

    public static abstract class CreateEntityTestSystem {

        private final List<String> order = new ArrayList<>();

        @All
        @Inserted
        public final void insertedAll(int entityId) {
            order.add("insertedAll");
        }

        @All(Position.class)
        @Inserted
        public final void insertedPos(int entityId) {
            order.add("insertedPos");
        }

        @All(Position.class)
        @None(Velocity.class)
        @Inserted
        public final void insertedPosOnly(int entityId) {
            order.add("insertedPosOnly");
        }

        @All(Velocity.class)
        @Inserted
        public final void insertedVelocity(int entityId) {
            order.add("insertedVelocity");
        }

        @Archetype
        abstract void createEntity(int count);

        @EntityInitializer
        final void createEntity(int index, Position pos) {
            order.add("createEntity");
        }

    }

}
