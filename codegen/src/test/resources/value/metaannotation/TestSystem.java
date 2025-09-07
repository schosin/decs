package foo;

import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;

public class TestSystem {

    @SystemProcessor
    final void system(@Delta int delta) {
    }
    
    @All
    @EntityProcessor
    final void entity(int entityId, @Delta int delta) {
    }

}
