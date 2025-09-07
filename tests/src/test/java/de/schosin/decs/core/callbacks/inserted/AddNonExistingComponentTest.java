package de.schosin.decs.core.callbacks.inserted;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class AddNonExistingComponentTest {

    @Test
    void testAddNonExistingComponent() {
        var world = (InternalWorld) World.builder()
                .add(AddNonExistingComponentTestSystem.class)
                .build();

        var system = world.getSystem(AddNonExistingComponentTestSystem.class);
        assertThat(system.order).isEmpty();

        system.createEntity(1);
        assertThat(system.order).containsExactly("createEntity");

        world.process();
        assertThat(system.order).containsExactly(
                "createEntity",
                "insertedAll",
                "insertedPos",
                "insertedPosOnly",
                "addVelocity",
                "process",
                "insertedVelocity",
                "insertedPosVelocity");
    }

    public static abstract class AddNonExistingComponentTestSystem {

        private final List<String> order = new ArrayList<>();

        @All(Position.class)
        @None(Velocity.class)
        @EntityProcessor
        final void process(Entity entity) {
            addVelocity(entity);
            order.add("process");
        }

        @All
        @Inserted
        public final void insertedAll(int entityId) {
            order.add("insertedAll");
        }

        @None({ Position.class, Velocity.class })
        @Inserted
        public final void insertedNone(int entityId) {
            order.add("insertedNone");
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

        @All(Velocity.class)
        @None(Position.class)
        @Inserted
        public final void insertedVelocityOnly(int entityId) {
            order.add("insertedVelocity");
        }

        @All({ Position.class, Velocity.class })
        @Inserted
        public final void insertedPosVelocity(int entityId) {
            order.add("insertedPosVelocity");
        }

        @Archetype
        abstract void createEntity(int count);

        @EntityInitializer
        final void createEntity(int index, Position pos) {
            order.add("createEntity");
        }

        @Transmute
        abstract void addVelocity(Entity entity);

        @EntityInitializer
        final void addVelocity(int entityId, Velocity velocity) {
            order.add("addVelocity");
        }

    }

}
