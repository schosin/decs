import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;

public abstract class TestSystem {

    @All
    @EntityProcessor
    final void entity(int entityId) {
    }

    @Archetype
    abstract int create(Data data);

}
