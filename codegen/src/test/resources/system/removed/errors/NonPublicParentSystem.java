package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

class NonPublicParentSystem {

    public static class ActualSystem {

        @All
        @Removed
        final void processEntity(int entityId) {
        }

    }

}
