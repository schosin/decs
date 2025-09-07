package foo;

import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;

public class MissingCompositionSystem {

    @Inserted
    final void inserted(int entityId) {
    }

    @EntityProcessor
    final void processEntity(int entityId) {
    }

    @Removed
    final void removed(int entityId) {
    }

}
