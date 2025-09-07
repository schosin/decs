package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class NonFinalEntityInitializerUtility {

    @Archetype
    abstract void archetype(int count);

    @EntityInitializer
    void archetype(int index, Position pos) {
    }

    @Transmute
    abstract void transmute(Entity entity);

    @EntityInitializer
    void transmute(int entityId, Position pos) {
    }

}
