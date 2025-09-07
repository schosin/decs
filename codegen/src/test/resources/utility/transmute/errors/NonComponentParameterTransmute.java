package foo;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.internal.InternalEntity;
import de.schosin.decs.api.internal.InternalWorld;

public abstract class NonComponentParameterTransmute {

    @Transmute
    public abstract void archetype(Entity entity);

    @EntityInitializer
    final void archetype(int entityId, ExampleArchetype archetype) {
    }

    @Transmute
    public abstract void transmute(Entity entity);

    @EntityInitializer
    final void transmute(int entityId, ExampleTransmute transmute) {
    }

    @Transmute
    public abstract void primitiveInt(Entity entity);

    @EntityInitializer
    final void primitiveInt(int entityId, int value) {
    }

    @Transmute
    public abstract void primitiveLong(Entity entity);

    @EntityInitializer
    final void primitiveLong(int entityId, long value) {
    }

    @Transmute
    public abstract void string(Entity entity);

    @EntityInitializer
    final void string(int entityId, String value) {
    }

    @Transmute
    public abstract void world(Entity entity);

    @EntityInitializer
    final void world(int entityId, World world) {
    }

    @Transmute
    public abstract void internalWorld(Entity entity);

    @EntityInitializer
    final void internalWorld(int entityId, InternalWorld world) {
    }

    @Transmute
    public abstract void entity(Entity entity);

    @EntityInitializer
    final void entity(int entityId, Entity entity) {
    }

    @Transmute
    public abstract void internalEntity(Entity entity);

    @EntityInitializer
    final void internalEntity(int entityId, InternalEntity entity) {
    }

}
