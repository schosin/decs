package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.internal.InternalWorld;

public class InternalWorldParameterSystem {

    @All
    @Inserted
    final void inserted(int entityId, InternalWorld world) {
    }

    @All
    @EntityProcessor
    final void processEntity(int entityId, InternalWorld world) {
    }

    @All
    @Removed
    final void removed(int entityId, InternalWorld world) {
    }

    @SystemProcessor
    final void system(InternalWorld world) {
    }

}
