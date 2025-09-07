package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

public class CallMethodSystem {

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

    public Void call() throws Exception {
        return null;
    }

}
