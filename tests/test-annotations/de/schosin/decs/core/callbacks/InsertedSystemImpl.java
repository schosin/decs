package de.schosin.decs.core.callbacks;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.utils.collections.IntBag;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class InsertedSystemImpl extends InsertedTest.InsertedSystem implements SystemType {
    private final InternalWorld _world;

    public InsertedSystemImpl(InternalWorld world) {
        this._world = world;
    }

    @Override
    public final void offerArchetype(EntityArchetype archetype) {
        if (InsertedAllInsertedHandler.COMPOSITION.matches(archetype)) {
            archetype.inserted(new InsertedAllInsertedHandler(this._world, archetype, this));
        }
    }

    @Override
    public final void runSystem() {
    }

    private static final class InsertedAllInsertedHandler implements EntityArchetypeListener {
        private static final Composition COMPOSITION = Composition.all().build();

        private final World _world;

        private final EntityArchetype _archetype;

        private final InsertedSystemImpl _system;

        private InsertedAllInsertedHandler(World world, EntityArchetype archetype,
                InsertedSystemImpl system) {
            this._world = world;
            this._archetype = archetype;
            this._system = system;
        }

        @Override
        public final void process(int _start, int _end) {
            int[] _data = this._archetype.getEntities().getData();

            for (int _i = _start; _i < _end; _i++) {
                _system.insertedAll(_data[_i]);
            }
        }

        @Override
        public final void process(IntBag _indices) {
            int[] _data = this._archetype.getEntities().getData();
            int[] _indicesData = _indices.getData();

            for (int _i = 0, _s = _indices.size(); _i < _s; _i++) {
                int _index = _indicesData[_i];

                this._system.insertedAll(_data[_index]);
            }
        }
    }
}
