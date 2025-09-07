package foo;

import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;

public class TestSystem {

    @SystemProcessor
    final void system(@Value("test") int value) {
    }

    @All
    @EntityProcessor
    final void entity(int entityId, @Value("test") float value) {
    }

    @SystemProcessor
    final void metaSystem(@Value("delta") String value) {
    }

    @All
    @EntityProcessor
    final void metaEntity(int entityId, @Delta Class<?> value) {
    }

}
