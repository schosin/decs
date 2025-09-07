package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class DuplicateComponentArchetype {

    @Archetype
    public abstract void create1(int count);

    @EntityInitializer
    final void create1(int index, Position pos, Velocity velocity, Position pos2) {
    }

    @Archetype
    public abstract void create2(int count);

    @EntityInitializer
    final void create2(int index, Position pos, Velocity velocity, Velocity velocity2) {
    }

}
