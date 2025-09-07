package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

// padding for missing Entity import
public enum EnumTypeSystem {

    INSTANCE;

    @Archetype
    void archetype(int count) {
    }

    @EntityInitializer
    final void archetype(int index, Position pos) {
    }

}
