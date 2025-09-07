package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

public class CallMethodSystem {

    @All
    @EntityProcessor
    final void processEntity(int entityId) {
    }

    public Void call() throws Exception {
        return null;
    }

}
