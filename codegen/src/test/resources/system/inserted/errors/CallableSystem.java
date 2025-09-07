package foo;

import java.util.concurrent.Callable;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;

public class CallableSystem implements Callable<Void> {

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

    @Override
    public Void call() throws Exception {
        return null;
    }

}
