package de.schosin.decs.api.entities;

import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.utils.Transmute;

/**
 * Base interface for {@link Entity} and {@link EntityRef} to support passing either to {@link Transmute @Transmute} methods.
 * 
 * <p>
 * This type should only be directly used as the first parameter of a {@link Transmute @Transmute} method when needing to pass either type. <br />
 * 
 * <p>
 * This interface intentionally does not declare any methods to avoid usage outside of {@link Transmute @Transmute} methods. <br />
 * {@link Entity} does not support usage outside its {@link EntityProcessor @EntityProcessor} method. <br />
 * {@link EntityRef} is meant to be {@link EntityRef#free() freed} when no longer needed. <br />
 * Moving {@link Entity#id()} or {@link Entity#delete()} to this interface could incentivise keeping references to {@link BaseEntity}, breaking both of these contracts.
 */
public interface BaseEntity {
}
