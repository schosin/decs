package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public @interface AnnotationTypeSystem {

    @SystemProcessor
    void processEntity();

}
