package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public enum EnumTypeSystem {
    
    INSTANCE;

    @All
    @Removed
    final void processEntity(int entityId) {
    }

}
