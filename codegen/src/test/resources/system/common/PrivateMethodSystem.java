package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.system.SystemProcessor;

public class PrivateMethodSystem {

    @All
    @Inserted
    private final void inserted(int entityId) {
    }

    @All
    @EntityProcessor
    private final void processEntity(int entityId) {
    }

    @All
    @Removed
    private final void removed(int entityId) {
    }

    @SystemProcessor
    private final void system() {
    }

}
