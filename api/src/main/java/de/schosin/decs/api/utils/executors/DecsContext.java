package de.schosin.decs.api.utils.executors;

import de.schosin.decs.api.systems.SystemInvocation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Offers {@link #isParallel()} to check whether the current method is running in a generated, parallel system.
 *
 * <p>
 * Checks whether the current thread is a {@link DecsThread}, which is used when an {@link ExecutorService} with {@link DecsThreadFactory} is used.
 * {@link DecsExecutors#newDefaultExecutor(Class, int)} is using {@link DecsThreadFactory} and is used by generated {@link SystemInvocation} implementations for parallel system invocation.
 */
public final class DecsContext {

    /**
     * Creates a new {@link ThreadFactory} that enables the use of {@link #isParallel()}.
     *
     * @param prefix prefix for thread names
     * @return thread factory
     */
    public static ThreadFactory createThreadFactory(String prefix) {
        return new DecsThreadFactory(prefix);
    }

    /**
     * Returns true if the current thread was spawned by a {@link ThreadFactory} from {@link #createThreadFactory(String)}.
     */
    public static boolean isParallel() {
        return Thread.currentThread() instanceof DecsThread;
    }

    private static final class DecsThreadFactory implements ThreadFactory {

        private final String prefix;

        DecsThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            return new DecsThread(prefix, task);
        }

    }

    private static final class DecsThread extends Thread {
        public DecsThread(String prefix, Runnable task) {
            super(task);

            setName(prefix + "-thread-" + getId());
        }
    }

    private DecsContext() {
    }

}
