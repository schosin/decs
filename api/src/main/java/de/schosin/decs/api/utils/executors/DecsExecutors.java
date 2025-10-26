package de.schosin.decs.api.utils.executors;

import de.schosin.decs.api.systems.SystemInvocation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Static factory methods for creating an {@link ExecutorService} that allows the use of {@link DecsContext#isParallel()}.
 * Used in generated code to submit parallel systems to the executor.
 */
public final class DecsExecutors {

    /**
     * Creates a new fixed thread executor with {@link Runtime#availableProcessors()}.
     *
     * @param threadPrefix prefix for the thread name
     * @return {@link ExecutorService} that enables the use of {@link DecsContext#isParallel()}
     */
    public static ExecutorService newDefaultExecutor(String threadPrefix) {
        return newDefaultExecutor(threadPrefix, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a new fixed thread executor with the configured number of threads.
     *
     * @param threadPrefix prefix for the thread name
     * @param nThreads number of threads for {@link Executors#newFixedThreadPool(int, ThreadFactory)}
     * @return {@link ExecutorService} that enables the use of {@link DecsContext#isParallel()}
     */
    public static ExecutorService newDefaultExecutor(String threadPrefix, int nThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, DecsContext.createThreadFactory(threadPrefix));

        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        return executor;
    }

    private DecsExecutors() {
    }

}
