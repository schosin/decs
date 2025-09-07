package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

public abstract class EntityIdParameterSystem {

    @SystemProcessor
    final void system(int entityId) {
    }

}
