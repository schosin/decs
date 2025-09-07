package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

public enum EnumTypeSystem {
    
    INSTANCE;

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

}
