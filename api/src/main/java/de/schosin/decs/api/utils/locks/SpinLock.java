package de.schosin.decs.api.utils.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Lock implementation based on an {@link AtomicInteger} that performs no yields.
 * 
 * <p>
 * Improves lock acquisition at the cost of higher CPU consumption.
 * 
 * @see YieldingSpinLock
 * @see WaitingSpinLock from "decs-core-11" for an optimized version using {@code Thread.onSpinWait()}
 */
public final class SpinLock implements Lock {

    private final AtomicInteger state = new AtomicInteger(0);

    @Override
    public void lock() {
        while (state.compareAndSet(0, 1)) {
            // Thread.onSpinWait() only in JDK 9+ 
        }
    }

    @Override
    public void unlock() {
        state.set(0);
    }

    @Override
    public boolean tryLock() {
        return state.compareAndSet(0, 1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(time);
        while (System.nanoTime() < deadline) {
            if (tryLock()) {
                return true;
            }

            // Thread.onSpinWait() only in JDK 9+ 
        }

        return false;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        while (!tryLock()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            // Thread.onSpinWait() only in JDK 9+ 
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("newCondition not supported");
    }

}
