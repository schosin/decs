package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

public class NonStaticNestedSystem {

    public class ActualSystem {

        @All
        @EntityProcessor
        final void processEntity(int entityId) {
        }

    }

}
