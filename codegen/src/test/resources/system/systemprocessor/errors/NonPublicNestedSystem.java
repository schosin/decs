package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public class NonPublicNestedSystem {

    static class ActualSystem {

        // padding for missing @All
        @SystemProcessor
        final void system() {
        }

    }

}
