package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class InvalidReturnTypeArchetype {

    @Archetype
    abstract long create(int count);
    
    @EntityInitializer
    final void create(int index, Position pos) {
    }

}
