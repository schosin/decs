package de.schosin.decs.core.transmute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.BaseEntity;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.entities.EntityRef;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.tests.components.Acceleration;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public class TransmuteEntityRefTest {

    @Test
    void testTransmuteInEntityProcessor() {
        var world = (InternalWorld) World.builder()
                .add(TransmuteEntityRefTestSystem1.class)
                .add(TransmuteEntityRefTestSystem2.class)
                .build();

        var archetype = world.getUtility(EntityRefTestTransmute.class);
        var system1 = world.getSystem(TransmuteEntityRefTestSystem1.class);
        var system2 = world.getSystem(TransmuteEntityRefTestSystem2.class);

        archetype.createPositioned(1);
        archetype.createAccelerated(1);

        assertThat(system1.posCount).isZero();
        assertThat(system1.bothCount).isZero();

        assertThat(system2.accelerationCount).isZero();
        assertThat(system2.bothCount).isZero();

        world.process();
        assertThat(system1.posCount).isOne();
        assertThat(system1.bothCount).isOne();

        assertThat(system2.accelerationCount).isOne();
        assertThat(system2.bothCount).isOne();
    }

    public static class TransmuteEntityRefTestSystem1 {

        private EntityRef ref;

        private int posCount;
        private int bothCount;

        @All(Position.class)
        @None(Velocity.class)
        @EntityProcessor
        public final void process(Entity entity, Position pos) {
            assertThat(this.ref).isNull();

            assertThat(pos).isNotNull();
            posCount++;

            this.ref = entity.createRef();
        }

        @SystemProcessor(modifying = true)
        public final void process(@Utility EntityRefTestTransmute transmute) {
            assertThat(this.ref).isNotNull();

            transmute.add(ref);
        }

        @All({ Position.class, Velocity.class })
        @EntityProcessor
        public final void process(int id, Position pos, Velocity velocity) {
            assertThat(pos).isNotNull();
            assertThat(velocity).isNotNull();

            bothCount++;
        }

    }

    public static class TransmuteEntityRefTestSystem2 {

        private EntityRef ref;

        private int accelerationCount;
        private int bothCount;

        @All(Acceleration.class)
        @None(Velocity.class)
        @EntityProcessor
        public final void process(Entity entity, Acceleration acceleration) {
            assertThat(this.ref).isNull();

            assertThat(acceleration).isNotNull();
            accelerationCount++;

            this.ref = entity.createRef();
        }

        @SystemProcessor(modifying = true)
        public final void process(@Utility EntityRefTestTransmute transmute) {
            assertThat(this.ref).isNotNull();

            transmute.add(ref);
        }

        @SystemProcessor
        public final void padding() {
            assertThat(this.ref).isNotNull();

            // just here to have another method in between the modifying @SystemProcessor and @EntityProcessor
        }

        @All({ Acceleration.class, Velocity.class })
        @EntityProcessor
        public final void process(int id, Acceleration acceleration, Velocity velocity) {
            assertThat(acceleration).isNotNull();
            assertThat(velocity).isNotNull();

            bothCount++;
        }

    }

    public abstract static class EntityRefTestTransmute {

        @Archetype
        abstract void createPositioned(int count);

        @EntityInitializer
        final void createPositioned(int index, Position pos) {
        }

        @Archetype
        abstract void createAccelerated(int count);

        @EntityInitializer
        final void createAccelerated(int index, Acceleration acceleration) {
        }

        @Transmute
        abstract void add(BaseEntity entity);

        @EntityInitializer
        final void add(int entityId, Velocity velocity) {
        }

    }

}
