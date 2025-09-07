package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

class NonPublicSystem {

    @All
    @EntityProcessor
    final void processEntity(int entityId) {
    }

}
