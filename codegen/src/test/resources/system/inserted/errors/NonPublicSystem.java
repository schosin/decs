package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

class NonPublicSystem {

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

}
