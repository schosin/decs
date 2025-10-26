package de.schosin.decs.api.utils.locks;

import de.schosin.decs.api.internal.InternalWorld;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 *
 * <p>
 * Class holding locks used by the generated for to synchronize write access.
 * An instance can be obtained with {@link InternalWorld#getLocks()}.
 */
public final class Locks {

    private static final Map<InternalWorld, Locks> LOCKS = Collections.synchronizedMap(new WeakHashMap<>());

    public static Locks getInstance(InternalWorld world) {
        return LOCKS.computeIfAbsent(world, Locks::new);
    }

    private final ClassValue<Lock> classLocks;
    private final Map<Object, Lock> identityLocks = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<Object, Lock> equalityLocks = new ConcurrentHashMap<>();

    private Locks(InternalWorld world) {
        this.classLocks = new ClassValue<Lock>() {
            @Override
            protected Lock computeValue(Class<?> type) {
                return createLock(type);
            }
        };
    }

    public Lock getLock(Class<?> key) {
        return classLocks.get(key);
    }

    public Lock getIdentityLock(Object key) {
        return identityLocks.computeIfAbsent(key, this::createLock);
    }

    public Lock getEqualityLock(Object key) {
        return equalityLocks.computeIfAbsent(key, this::createLock);
    }

    private Lock createLock(Object key) {
        // TOOD from world configuration
        return new ReentrantLock();
    }

}
