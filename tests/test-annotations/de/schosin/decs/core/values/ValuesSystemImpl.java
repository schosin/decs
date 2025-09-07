package de.schosin.decs.core.values;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.tests.components.Velocity;
import de.schosin.decs.values.Values;
import java.util.List;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class ValuesSystemImpl extends ValuesTest2.ValuesSystem implements SystemType {
    private final InternalWorld _world;

    private final Values _values;

    private final Bag<ProcessA> processA;

    private final Bag<ProcessB> processB;

    public ValuesSystemImpl(InternalWorld world) {
        this._world = world;
        this._values = world.getValues();
        this.processA = new Bag<>(ProcessA.class, 4);
        this.processB = new Bag<>(ProcessB.class, 4);
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
        if (ProcessA.COMPOSITION.matches(archetype)) {
            this.processA.add(new ProcessA(this._world, archetype, this));
        }
        if (ProcessB.COMPOSITION.matches(archetype)) {
            this.processB.add(new ProcessB(this._world, archetype, this));
        }
    }

    @Override
    public final void runSystem() {
        // sysA(float foo, List)
        sysA(_values.aaa, _values.bbb);

        // processA(int entityId, float foo, List)
        ProcessA[] processAData = this.processA.getData();
        for (int i = 0, s = this.processA.size(); i<s; i++) {
            processAData[i].run();
        }

        // processB(int entityId, float foo, List)
        ProcessB[] processBData = this.processB.getData();
        for (int i = 0, s = this.processB.size(); i<s; i++) {
            processBData[i].run();
        }

        // sysB(float foo, List)
        sysB(_values.aaa, _values.bbb);
    }

    private static final class ProcessA {
        private static final Composition COMPOSITION = Composition.all().build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final ValuesSystemImpl _system;

        private final Values _values;

        private ProcessA(World world, EntityArchetype archetype, ValuesSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this._values = world.getValues();
        }

        private final void run() {
            float foo = _values.aaa;
            List bar = _values.bbb;

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._system.processA(_data[_i], foo, bar);
            }
        }
    }

    private static final class ProcessB {
        private static final Composition COMPOSITION = Composition
            .all(Velocity.class)
            .build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final ValuesSystemImpl _system;

        private final Values _values;

        private ProcessB(World world, EntityArchetype archetype, ValuesSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;

            this._values = world.getValues();
        }

        private final void run() {
            float foo = _values.aaa;
            List bar = _values.bbb;

            int[] _data = this._archetype.getEntities().getData();

            // Iterate entities
            for (int _i = 0, _s = this._archetype.getAlive(); _i < _s; _i++) {
                this._system.processB(_data[_i], foo, bar);
            }
        }
    }
}
