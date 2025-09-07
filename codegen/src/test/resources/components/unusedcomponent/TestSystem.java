package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class TestSystem {

    @All
    @EntityProcessor
    final void entity(int entityId, Position pos) {
    }

    @Archetype
    abstract void create(int count);

    @EntityInitializer
    final void create(int index, Position pos, Velocity vel) {
    }

}
