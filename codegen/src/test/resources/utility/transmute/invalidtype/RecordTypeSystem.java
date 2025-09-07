package foo;

import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public record RecordTypeSystem() {

    @Transmute
    void transmute(Entity entity) {
    }

    @EntityInitializer
    final void transmute(int entityId, Position pos) {
    }

}
