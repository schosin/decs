package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public @interface AnnotationTypeSystem {

    @All
    @Removed
    void processEntity();

}
