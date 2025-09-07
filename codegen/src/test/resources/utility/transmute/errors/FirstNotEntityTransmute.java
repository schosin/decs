package foo;

import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;

public abstract class FirstNotEntityTransmute {

    @Transmute
    abstract void transmute1();

    @EntityInitializer
    final void transmute1(int entityId, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute2(String param);

    @EntityInitializer
    final void transmute2(int entityId, String param, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute3(String param, Entity entity);

    @EntityInitializer
    final void transmute3(int entityId, String param, Entity entity, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute4(Entity entity);

    @EntityInitializer
    final void transmute4(Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute5(Entity entity, String param);

    @EntityInitializer
    final void transmute5(String param, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute6(Entity entity, String param);

    @EntityInitializer
    final void transmute6(String param, int entityId, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute7(Entity entitaet);

    @EntityInitializer
    final void transmute7(int entityId, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute8(Entity entity);

    @EntityInitializer
    final void transmute8(int id, Position pos, Velocity velocity) {
    }

    @Transmute
    abstract void transmute9(Entity entity);

    @EntityInitializer
    final void transmute9() {
    }

}
