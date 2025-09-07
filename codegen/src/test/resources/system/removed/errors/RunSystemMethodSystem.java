package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public class RunSystemMethodSystem {

    @All
    @Removed
    final void processEntity(int entityId) {
    }

    public final void runSystem() {
    }

}
