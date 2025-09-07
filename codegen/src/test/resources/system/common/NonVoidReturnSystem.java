package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.system.SystemProcessor;

public class NonVoidReturnSystem {

    @All
    @Inserted
    final int inserted(int entityId) {
        return 42;
    }

    @All
    @EntityProcessor
    final int processEntity(int entityId) {
        return 42;
    }

    @All
    @Removed
    final int removed(int entityId) {
        return 42;
    }

    @SystemProcessor
    final int system() {
        return 42;
    }

}
