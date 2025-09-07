package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public class CallMethodSystem {

    @All
    @Removed
    final void processEntity(int entityId) {
    }

    public Void call() throws Exception {
        return null;
    }

}
