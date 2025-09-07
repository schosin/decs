package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

public interface InterfaceTypeSystem {

    @All
    @Inserted
    void processEntity(int entityId);

}
