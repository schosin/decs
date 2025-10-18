package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;

public abstract class System {

    @All({ Position.class, Velocity.class })
    @EntityProcessor
    final void process(Position pos, Velocity velocity) {
        pos.x(pos.x() + velocity.vx());
        pos.y(pos.y() + velocity.vy());
    }

    @All({ Position.class, Velocity.class })
    @EntityProcessor
    final void processDefault(Position pos, Velocity velocity) {
        pos.add(velocity.vx(), velocity.vy());
    }

    @Archetype
    abstract void create(int count);

    @EntityInitializer
    final void create(int index, Position pos, Velocity velocity) {
    }

}
