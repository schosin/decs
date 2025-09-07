package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public class NonStaticNestedSystem {

    public class ActualSystem {

        // padding for missing @All
        @SystemProcessor
        final void system() {
        }

    }

}
