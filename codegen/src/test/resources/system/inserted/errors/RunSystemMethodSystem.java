package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

public class RunSystemMethodSystem {

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

    public final void runSystem() {
    }

}
