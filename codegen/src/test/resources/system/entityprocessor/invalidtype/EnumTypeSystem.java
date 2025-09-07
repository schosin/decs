package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

public enum EnumTypeSystem {
    
    INSTANCE;

    @All
    @EntityProcessor
    final void processEntity(int entityId) {
    }

}
