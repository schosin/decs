package foo;

import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class AddRemoveSameComponentSystem {

    @Transmute(remove = Position.class)
    abstract void transmute1(Entity entity);

    @EntityInitializer
    final void transmute1(int entityId, Position pos) {
    }

    @Transmute(remove = Position.class)
    abstract void transmute2(Entity entity);

    @EntityInitializer
    final void transmute2(int entityId, Position pos, Velocity velocity) {
    }

    @Transmute(remove = Velocity.class)
    abstract void transmute3(Entity entity);

    @EntityInitializer
    final void transmute3(int entityId, Position pos, Velocity velocity) {
    }

    @Transmute(remove = { Position.class, Velocity.class })
    abstract void transmute4(Entity entity);

    @EntityInitializer
    final void transmute4(int entityId, Position pos, Velocity velocity) {
    }

}
