package de.schosin.decs.core.spi;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import de.schosin.decs.api.builder.WorldBuilder;

/**
 * Factory SPI for the default entities lock matching {@link WorldBuilder#entitiesLocks(int, Supplier)}.
 */
public interface EntitiesLockSupplierFactory extends Supplier<Lock> {
}
