package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.system.SystemProcessor;

public class NonFinalMethodSystem {

    @All
    @Inserted
    void inserted(int entityId) {
    }

    @All
    @EntityProcessor
    void processEntity(int entityId) {
    }

    @All
    @Removed
    void removed(int entityId) {
    }

    @SystemProcessor
    void system() {
    }

}
