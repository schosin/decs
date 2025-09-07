package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class SimpleArchetype {

    @Archetype
    public abstract void create(int count);

    @EntityInitializer
    protected final void create(int index, Position pos, Velocity velocity) {
    }

}
