package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public class NonAbstractUtilitySystem {

    @All
    @EntityProcessor
    final void processEntity(int entityId) {
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
