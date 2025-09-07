import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

public abstract class TestSystem {

    @All
    @EntityProcessor
    final void entity(int entityId, Data data) {
    }

}
