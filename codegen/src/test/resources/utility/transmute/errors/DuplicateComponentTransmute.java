package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class DuplicateComponentTransmute {

    @Transmute
    abstract void transmute1(Entity entity);

    @EntityInitializer
    final void transmute1(int entityId, Position pos, Velocity velocity, Position pos2) {
    }

    @Transmute
    abstract void transmute2(Entity entity);

    @EntityInitializer
    final void transmute2(int entityId, Position pos, Velocity velocity, Velocity velocity2) {
    }

}
