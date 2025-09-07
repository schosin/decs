package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public final class FinalClassTypeSystem {

    @All
    @Removed
    final void processEntity(int entityId) {
    }

}
