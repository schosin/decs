package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public record RecordTypeSystem() {

    @SystemProcessor
    final void processEntity(int entityId) {
    }

}
