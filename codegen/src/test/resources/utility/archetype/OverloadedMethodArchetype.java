package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class OverloadedMethodArchetype {

    @Archetype
    public abstract void create(int count, String p1);

    @EntityInitializer
    protected final void create(int index, String p1, Position pos) {
    }

    @Archetype
    public abstract void create(int count, String p1, String p2);

    @EntityInitializer
    protected final void create(int index, String p1, String p2, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create(int count, String p1, String p2, String p3);

    @EntityInitializer
    protected final void create(int index, String p1, String p2, String p3, Position pos, Velocity velocity, Acceleration acceleration) {
    }

}
