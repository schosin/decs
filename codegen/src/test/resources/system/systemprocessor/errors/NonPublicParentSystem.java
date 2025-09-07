package foo;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
class NonPublicParentSystem {

    public static class ActualSystem {

        // padding for missing @All
        @SystemProcessor
        final void system() {
        }

    }

}
