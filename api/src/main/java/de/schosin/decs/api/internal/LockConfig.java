package de.schosin.decs.api.internal;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import de.schosin.decs.api.builder.WorldBuilder;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Object for holding the configuration provided by {@link WorldBuilder#entitiesLocks(int, Supplier)}.
 */
public final class LockConfig {

    private final int entitiesLockSize;
    private final Supplier<Lock> entitiesLockSupplier;

    public LockConfig(int entitiesLockSize, Supplier<Lock> entitiesLockSupplier) {
        this.entitiesLockSize = entitiesLockSize;
        this.entitiesLockSupplier = entitiesLockSupplier;
    }

    public int getEntitiesLockSize() {
        return this.entitiesLockSize;
    }

    public Supplier<Lock> getEntitiesLockSupplier() {
        return this.entitiesLockSupplier;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LockConfig [entitiesLockSize=").append(this.entitiesLockSize).append(", entitiesLockSupplier=").append(this.entitiesLockSupplier).append("]");
        return builder.toString();
    }

}
