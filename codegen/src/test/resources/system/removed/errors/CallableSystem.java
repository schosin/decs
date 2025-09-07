package foo;

import java.util.concurrent.Callable;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Removed;

public class CallableSystem implements Callable<Void> {

    @All
    @Removed
    final void processEntity(int entityId) {
    }

    @Override
    public Void call() throws Exception {
        return null;
    }

}
