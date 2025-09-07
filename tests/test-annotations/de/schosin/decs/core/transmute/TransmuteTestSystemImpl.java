package de.schosin.decs.core.transmute;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalEntity;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class TransmuteTestSystemImpl extends TransmuteTest.TransmuteTestSystem implements SystemType {
    private final InternalWorld _world;

    private final TransmuteTest.TestTransmute testTransmute;

    private final Bag<Process> process;

    private final Bag<Process2> process2;

    public TransmuteTestSystemImpl(InternalWorld world) {
        this._world = world;
        this.testTransmute = world.getUtility(TransmuteTest.TestTransmute.class);
        this.process = new Bag<>(Process.class, 4);
        this.process2 = new Bag<>(Process2.class, 4);
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
        if (Process.COMPOSITION.matches(archetype)) {
            this.process.add(new Process(this._world, archetype, this));
        }
        if (Process2.COMPOSITION.matches(archetype)) {
            this.process2.add(new Process2(this._world, archetype, this));
        }
    }

    @Override
    public final void runSystem() {
        // process(Entity, Position, TestTransmute)
        Process[] processData = this.process.getData();
        for (int i = 0, s = this.process.size(); i<s; i++) {
            processData[i].run();
        }

        // Flush potential entity changes by "this.process"
        this._world.flushChanges();

        // process(int id, Position, Velocity)
        Process2[] process2Data = this.process2.getData();
        for (int i = 0, s = this.process2.size(); i<s; i++) {
            process2Data[i].run();
        }
    }

    private static final class Process implements InternalEntity {
        private static final Composition COMPOSITION = Composition
            .all(Position.class)
            .none(Velocity.class)
            .build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final TransmuteTestSystemImpl _system;

        private final TransmuteTest.TestTransmute testTransmute;

        private final Bag<Position> positionComponents;

        private int _index = -1;

        private int _entityId = -1;

        private Process(World world, EntityArchetype archetype, TransmuteTestSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this.testTransmute = world.getUtility(TransmuteTest.TestTransmute.class);

            this.positionComponents = archetype.getData(Position.class);
        }

        private final void run() {
            Position[] pos = this.positionComponents.getData();

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._index = _i;
                this._entityId = _data[_i];

                this._system.process(this, pos[_i], testTransmute);
            }

            // Reset state to avoid improper use as Entity outside of the loop
            this._index = -1;
            this._entityId = -1;
        }

        @Override
        public final int id() {
            return this._entityId;
        }

        @Override
        public final void delete() {
            this._archetype.deleteEntity(this._index);
        }

        @Override
        public final int index() {
            return this._index;
        }

        @Override
        public final EntityArchetype archetype() {
            return this._archetype;
        }

        @Override
        public final String toString() {
            return new StringBuilder().append("Entity(id = ").append(this._entityId).append(")").toString();
        }
    }

    private static final class Process2 {
        private static final Composition COMPOSITION = Composition
            .all(Position.class, Velocity.class)
            .build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final TransmuteTestSystemImpl _system;

        private final Bag<Position> positionComponents;

        private final Bag<Velocity> velocityComponents;

        private Process2(World world, EntityArchetype archetype, TransmuteTestSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this.positionComponents = archetype.getData(Position.class);
            this.velocityComponents = archetype.getData(Velocity.class);
        }

        private final void run() {
            Position[] pos = this.positionComponents.getData();
            Velocity[] velocity = this.velocityComponents.getData();

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._system.process(_data[_i], pos[_i], velocity[_i]);
            }
        }
    }
}
