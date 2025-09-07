package foo;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.internal.InternalEntity;
import de.schosin.decs.api.internal.InternalWorld;

public abstract class NonComponentParameterArchetype {

    @Archetype
    public abstract void archetype(int count);

    @EntityInitializer
    final void archetype(int index, ExampleArchetype archetype) {
    }

    @Archetype
    public abstract void transmute(int count);

    @EntityInitializer
    final void transmute(int index, ExampleTransmute transmute) {
    }

    @Archetype
    public abstract void primitiveInt(int count);

    @EntityInitializer
    final void primitiveInt(int index, int value) {
    }

    @Archetype
    public abstract void primitiveLong(int count);

    @EntityInitializer
    final void primitiveLong(int index, long value) {
    }

    @Archetype
    public abstract void string(int count);

    @EntityInitializer
    final void string(int index, String value) {
    }

    @Archetype
    public abstract void world(int count);

    @EntityInitializer
    final void world(int index, World world) {
    }

    @Archetype
    public abstract void internalWorld(int count);

    @EntityInitializer
    final void internalWorld(int index, InternalWorld world) {
    }

    @Archetype
    public abstract void entity(int count);

    @EntityInitializer
    final void entity(int index, Entity entity) {
    }

    @Archetype
    public abstract void internalEntity(int count);

    @EntityInitializer
    final void internalEntity(int index, InternalEntity entity) {
    }

}
