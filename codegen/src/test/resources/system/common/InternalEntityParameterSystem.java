package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.internal.InternalEntity;
import de.schosin.decs.api.internal.InternalWorld;

public class InternalEntityParameterSystem {

    @All
    @Inserted
    final void inserted(int entityId, InternalEntity entity) {
    }

    @All
    @EntityProcessor
    final void processEntity(int entityId, InternalEntity entity) {
    }

    @All
    @Removed
    final void removed(int entityId, InternalEntity entity) {
    }

}
