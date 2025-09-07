package foo;

import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class OverloadedMethodTransmute {

    @Transmute
    abstract void transmute(Entity entity);

    @EntityInitializer final 
    void transmute(int entityId, Position pos) {}

    @Transmute
    abstract void transmute(Entity entity, String foo);

    @EntityInitializer final 
    void transmute(int entityId, String foo, Position pos, Velocity velocity) {}

    @Transmute
    abstract void transmute(Entity entity, String foo, String bar);

    @EntityInitializer final 
    void transmute(int entityId, String foo, String bar, Position pos, Velocity velocity, Acceleration acceleration) {}

}
