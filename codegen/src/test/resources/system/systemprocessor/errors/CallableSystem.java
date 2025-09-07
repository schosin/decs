package foo;

import java.util.concurrent.Callable;

import de.schosin.decs.api.annotations.system.SystemProcessor;

// padding for missing @All
public class CallableSystem implements Callable<Void> {

    // padding for missing @All
    @SystemProcessor
    final void system() {
    }

    @Override
    public Void call() throws Exception {
        return null;
    }

}
