package foo;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;

public class NoEntityDataSystem {

    @All
    @Inserted
    final void inserted(World world) {
    }

    @All
    @EntityProcessor
    final void processEntity(World world) {
    }

    @All
    @Removed
    final void removed(World world) {
    }

}
