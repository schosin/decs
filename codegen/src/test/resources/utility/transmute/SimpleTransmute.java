package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class SimpleTransmute {

    @Transmute(remove = Acceleration.class)
    public abstract void transmute(Entity entity);

    @EntityInitializer
    protected final void transmute(int entityId, Position pos, Velocity velocity) {
    }

}
