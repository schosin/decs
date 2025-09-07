package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;

public @interface AnnotationTypeSystem {

    @All
    @EntityProcessor
    void processEntity();

}
