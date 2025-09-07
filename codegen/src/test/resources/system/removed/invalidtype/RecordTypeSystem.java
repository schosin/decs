package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public record RecordTypeSystem() {

    @All
    @Removed
    final void processEntity(int entityId) {
    }

}
