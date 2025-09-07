package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public interface InterfaceTypeSystem {

    @SystemProcessor
    void processEntity(int entityId);

}
