package foo;

import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public interface InterfaceTypeSystem {

    @Transmute
    void transmute(Entity entity);

    @EntityInitializer
    default void transmute(int entityId, Position pos, Velocity velocity) {
    }

}
