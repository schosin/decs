package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

public final class FinalClassTypeSystem {

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

}
