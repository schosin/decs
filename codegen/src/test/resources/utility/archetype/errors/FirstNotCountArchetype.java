package foo;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class FirstNotCountArchetype {

    @Archetype
    public abstract void create1();

    @EntityInitializer
    protected final void create1(int index, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create2(String param);

    @EntityInitializer
    protected final void create2(int index, String param, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create3(String param, int count);

    @EntityInitializer
    protected final void create3(int index, String param, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create4(String param, int count);

    @EntityInitializer
    protected final void create4(int index, String param, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create5(int anzahl);

    @EntityInitializer
    protected final void create5(int index, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create6(int count);

    @EntityInitializer
    protected final void create6(int idx, Position pos, Velocity velocity) {
    }

    @Archetype
    public abstract void create7(int count);

    @EntityInitializer
    protected final void create7() {
    }

}
