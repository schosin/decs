package de.schosin.decs.benchmark.system.entityprocessor;

import com.artemis.ArchetypeBuilder;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.WorldConfigurationBuilder;
import com.artemis.systems.IteratingSystem;
import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.EntityProcessor.ParallelStrategy;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.benchmark.BaseBenchmark;
import de.schosin.decs.benchmark.components.artemisodb.ArtemisPosition;
import de.schosin.decs.benchmark.components.artemisodb.ArtemisVelocity;
import de.schosin.decs.benchmark.components.decs.Marker1;
import de.schosin.decs.benchmark.components.decs.Marker2;
import de.schosin.decs.benchmark.components.decs.Marker3;
import de.schosin.decs.benchmark.components.decs.Marker4;
import de.schosin.decs.benchmark.components.decs.Position;
import de.schosin.decs.benchmark.components.decs.PositionInterface;
import de.schosin.decs.benchmark.components.decs.Velocity;
import de.schosin.decs.benchmark.components.decs.VelocityInterface;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.random.RandomGenerator;

public class EntityProcessorBenchmark extends BaseBenchmark {

    private static final RandomGenerator RNG = RandomGenerator.getDefault();

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(ClassComponentBenchmark.class))
                .include(benchmarkName(InterfaceComponentBenchmark.class))
                .include(benchmarkName(InterfaceDefaultComponentBenchmark.class))
                .include(benchmarkName(InterfaceInlineComponentBenchmark.class))
                .include(benchmarkName(InterfaceInlineOptimizedComponentBenchmark.class))
                .include(benchmarkName(ParallelInterfaceInlineOptimizedComponentBenchmark.class))
                .include(benchmarkName(ArtemisOdbIteratingSystemBenchmark.class))
                .include(benchmarkName(ArtemisOdbBaseSystemBenchmark.class))
                .build();

        new Runner(options).run();
    }

    @Param({ "1000000" })
    int numEntities;

    public static abstract class DecsBenchmark extends EntityProcessorBenchmark {

        World world;

    }

    public static class ClassComponentBenchmark extends DecsBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = World.builder()
                    .add(ClassProcessingSystem.class)
                    .build();

            var entities = world.getUtility(Entities.class);
            entities.createClass(numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class InterfaceComponentBenchmark extends DecsBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = World.builder()
                    .add(InterfaceProcessingSystem.class)
                    .build();

            var entities = world.getUtility(Entities.class);
            entities.createInterface(numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class InterfaceDefaultComponentBenchmark extends DecsBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = World.builder()
                    .add(InterfaceDefaultProcessingSystem.class)
                    .build();

            var entities = world.getUtility(Entities.class);
            entities.createInterface(numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class InterfaceInlineComponentBenchmark extends DecsBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = World.builder()
                    .add(InterfaceInlineProcessingSystem.class)
                    .build();

            var entities = world.getUtility(Entities.class);
            entities.createInterface(numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class InterfaceInlineOptimizedComponentBenchmark extends DecsBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = World.builder()
                    .add(InterfaceInlineOptimizedProcessingSystem.class)
                    .build();

            var entities = world.getUtility(Entities.class);
            entities.createInterface(numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class ParallelInterfaceInlineOptimizedComponentBenchmark extends DecsBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = World.builder()
                    .add(ParallelInterfaceInlineOptimizedProcessingSystem.class)
                    .build();

            var count = numEntities / 4;

            var entities = world.getUtility(Entities.class);
            entities.createInterface(count, Marker1.INSTANCE);
            entities.createInterface(count, Marker2.INSTANCE);
            entities.createInterface(count, Marker3.INSTANCE);
            entities.createInterface(count, Marker4.INSTANCE);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class ClassProcessingSystem {

        @All({ Position.class, Velocity.class })
        @EntityProcessor
        final void process(Position pos, Velocity velocity) {
            pos.x += velocity.vx;
            pos.y += velocity.vy;
        }

    }

    public static class InterfaceProcessingSystem {

        @All({ PositionInterface.class, VelocityInterface.class })
        @EntityProcessor
        final void process(PositionInterface pos, VelocityInterface velocity) {
            pos.x(pos.x() + velocity.vx());
            pos.y(pos.y() + velocity.vy());
        }

    }

    public static class InterfaceDefaultProcessingSystem {

        @All({ PositionInterface.class, VelocityInterface.class })
        @EntityProcessor
        final void process(PositionInterface pos, VelocityInterface velocity) {
            pos.add(velocity.vx(), velocity.vy());
        }

    }

    public static class InterfaceInlineProcessingSystem {

        @All({ PositionInterface.class, VelocityInterface.class })
        @EntityProcessor
        @de.schosin.decs.api.annotations.experimental.Inline
        final void process(PositionInterface pos, VelocityInterface velocity) {
            pos.x(pos.x() + velocity.vx());
            pos.y(pos.y() + velocity.vy());
        }

    }

    public static class InterfaceInlineOptimizedProcessingSystem {

        @All({ PositionInterface.class, VelocityInterface.class })
        @EntityProcessor
        @de.schosin.decs.api.annotations.experimental.Inline(optimize = true)
        final void process(PositionInterface pos, VelocityInterface velocity) {
            pos.x(pos.x() + velocity.vx());
            pos.y(pos.y() + velocity.vy());
        }

    }

    public static class ParallelInterfaceInlineOptimizedProcessingSystem {

        @All({ PositionInterface.class, VelocityInterface.class })
        @EntityProcessor(parallel = ParallelStrategy.PER_ARCHETYPE)
        @de.schosin.decs.api.annotations.experimental.Inline(optimize = true)
        final void process(PositionInterface pos, VelocityInterface velocity) {
            pos.x(pos.x() + velocity.vx());
            pos.y(pos.y() + velocity.vy());
        }

    }

    public static abstract class Entities {

        @Archetype
        abstract void createClass(int count);

        @EntityInitializer
        final void createClass(int index, Position pos, Velocity velocity) {
            pos.x = 0f;
            pos.y = 0f;

            velocity.vx = -1f + 2 * RNG.nextFloat();
            velocity.vy = -1f + 2 * RNG.nextFloat();
        }

        @Archetype
        abstract void createInterface(int count);

        @EntityInitializer
        final void createInterface(int index, PositionInterface pos, VelocityInterface velocity) {
            pos.x(0f);
            pos.y(0f);

            velocity.vx(-1f + 2 * RNG.nextFloat());
            velocity.vy(-1f + 2 * RNG.nextFloat());
        }

        @Archetype
        abstract void createInterface(int count, Marker1 marker);

        @EntityInitializer
        final void createInterface(int index, Marker1 marker, PositionInterface pos, VelocityInterface velocity) {
            pos.x(0f);
            pos.y(0f);

            velocity.vx(-1f + 2 * RNG.nextFloat());
            velocity.vy(-1f + 2 * RNG.nextFloat());
        }

        @Archetype
        abstract void createInterface(int count, Marker2 marker);

        @EntityInitializer
        final void createInterface(int index, Marker2 marker, PositionInterface pos, VelocityInterface velocity) {
            pos.x(0f);
            pos.y(0f);

            velocity.vx(-1f + 2 * RNG.nextFloat());
            velocity.vy(-1f + 2 * RNG.nextFloat());
        }

        @Archetype
        abstract void createInterface(int count, Marker3 marker);

        @EntityInitializer
        final void createInterface(int index, Marker3 marker, PositionInterface pos, VelocityInterface velocity) {
            pos.x(0f);
            pos.y(0f);

            velocity.vx(-1f + 2 * RNG.nextFloat());
            velocity.vy(-1f + 2 * RNG.nextFloat());
        }

        @Archetype
        abstract void createInterface(int count, Marker4 marker);

        @EntityInitializer
        final void createInterface(int index, Marker4 marker, PositionInterface pos, VelocityInterface velocity) {
            pos.x(0f);
            pos.y(0f);

            velocity.vx(-1f + 2 * RNG.nextFloat());
            velocity.vy(-1f + 2 * RNG.nextFloat());
        }

    }

    public static abstract class ArtemisOdbBenchmark extends EntityProcessorBenchmark {

        com.artemis.World world;

        protected static void createEntities(com.artemis.World world, int count) {
            var posM = world.getMapper(ArtemisPosition.class);
            var velocityM = world.getMapper(ArtemisVelocity.class);

            var archetype = new ArchetypeBuilder()
                    .add(ArtemisPosition.class)
                    .add(ArtemisVelocity.class)
                    .build(world);

            for (int i = 0; i < count; i++) {
                var entityId = world.create(archetype);

                var pos = posM.get(entityId);
                pos.x = 0f;
                pos.y = 0f;

                var velocity = velocityM.get(entityId);
                velocity.vx = -1f + 2 * RNG.nextFloat();
                velocity.vy = -1f + 2 * RNG.nextFloat();
            }
        }

    }

    public static class ArtemisOdbIteratingSystemBenchmark extends ArtemisOdbBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = new com.artemis.World(new WorldConfigurationBuilder()
                    .with(new ArtemisOdbIteratingSystem())
                    .build());

            createEntities(world, numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    public static class ArtemisOdbBaseSystemBenchmark extends ArtemisOdbBenchmark {

        @Setup(Level.Trial)
        public void setup() {
            world = new com.artemis.World(new WorldConfigurationBuilder()
                    .with(new ArtemisOdbBaseSystem())
                    .build());

            createEntities(world, numEntities);
        }

        @Benchmark
        public void process() {
            world.process();
        }

    }

    @com.artemis.annotations.All({ ArtemisPosition.class, ArtemisVelocity.class })
    static final class ArtemisOdbIteratingSystem extends IteratingSystem {

        ComponentMapper<ArtemisPosition> posM;
        ComponentMapper<ArtemisVelocity> velocityM;

        @Override
        protected void process(int entityId) {
            var pos = posM.get(entityId);
            var velocity = velocityM.get(entityId);

            pos.x += velocity.vx;
            pos.y += velocity.vy;
        }

    }

    // manual implementation of roughly what bytecode weaving does
    static final class ArtemisOdbBaseSystem extends BaseSystem {

        @com.artemis.annotations.All({ ArtemisPosition.class, ArtemisVelocity.class })
        EntitySubscription subscription;

        ComponentMapper<ArtemisPosition> posM;
        ComponentMapper<ArtemisVelocity> velocityM;

        @Override
        protected void processSystem() {
            var entities = subscription.getEntities();
            var data = entities.getData();

            for (int i = 0, s = entities.size(); i < s; i++) {
                var entityId = data[i];

                var pos = posM.get(entityId);
                var velocity = velocityM.get(entityId);

                pos.x += velocity.vx;
                pos.y += velocity.vy;
            }
        }

    }

}
