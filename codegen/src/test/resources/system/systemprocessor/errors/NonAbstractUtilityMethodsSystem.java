package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

// padding for missing @All
public abstract class NonAbstractUtilityMethodsSystem {

    // padding for missing @All
    @SystemProcessor
    final void process() {
    }

    @Archetype
    void archetype(int count) {
    }

    @EntityInitializer
    final void archetype(int index, Position pos) {
    }

    @Transmute
    void transmute(Entity entity) {
    }

    @EntityInitializer
    final void transmute(int entityId, Position pos) {
    }

}
