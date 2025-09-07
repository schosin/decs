package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

// padding for missing Entity import
public interface InterfaceTypeSystem {

    @Archetype
    void archetype(int count);

    @EntityInitializer
    default void archetype(int index, Position pos, Velocity velocity) {
    }

}
