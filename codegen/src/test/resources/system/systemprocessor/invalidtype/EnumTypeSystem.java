package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public enum EnumTypeSystem {
    
    INSTANCE;

    @SystemProcessor
    final void processEntity(int entityId) {
    }

}
