package bar;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class FooTestSystem {

    @All
    @EntityProcessor
    final void entity(int entityId, Data data) {
    }

    @Archetype
    abstract void create(int count);

    @EntityInitializer
    final void create(int index, Data data) {
    }

}
