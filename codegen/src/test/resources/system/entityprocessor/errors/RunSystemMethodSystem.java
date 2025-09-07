package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

public class RunSystemMethodSystem {

    @All
    @EntityProcessor
    final void processEntity(int entityId) {
    }

    public final void runSystem() {
    }

}
