package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.BaseEntity;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.entities.EntityRef;

public abstract class SimpleTransmute {

    @Transmute(remove = Acceleration.class)
    public abstract void transmute1(BaseEntity entity);

    @EntityInitializer
    protected final void transmute1(int entityId, Position pos, Velocity velocity) {
    }

    @Transmute(remove = Acceleration.class)
    public abstract void transmute2(Entity entity);

    @EntityInitializer
    protected final void transmute2(int entityId, Position pos, Velocity velocity) {
    }

    @Transmute(remove = Acceleration.class)
    public abstract void transmute3(EntityRef entity);

    @EntityInitializer
    protected final void transmute3(int entityId, Position pos, Velocity velocity) {
    }

}
