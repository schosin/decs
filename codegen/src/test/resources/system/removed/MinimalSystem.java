package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public class MinimalSystem {

    @All
    @Removed
    final void processEntity(int entityId) {
    }

}
