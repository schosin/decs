package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.entities.Entity;

public abstract class EntityParameterSystem {

    @SystemProcessor
    final void system(Entity entity) {
    }

}
