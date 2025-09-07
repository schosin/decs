package foo;

import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class ExampleTransmute {

    @Transmute
    abstract void transmute(Entity entity);

    @EntityInitializer
    final void transmute(int entityId, Position pos) {}

}
