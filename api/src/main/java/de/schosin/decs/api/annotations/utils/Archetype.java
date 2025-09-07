package de.schosin.decs.api.annotations.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.system.EntityProcessor;

/**
 * Annotation that can be used on abstract methods to create entities.
 * Can be used on either an interface or a system class.
 * The generated implementation must be able to access all involved types, including the component parameters and the interface or system class.
 * 
 * <p>
 * Method parameters may only be allowed component types (class, enum, record ) and must not be generic.
 * Passed components must not be null.
 * 
 * <p>
 * Methods must either have {@code void} or {@code int} (id of entity) as a return type.
 * 
 * <p>
 * <b>Attention:</b>
 * Every system must either inject a {@link Utility @Utility} or declare its own methods. 
 * Failing to do so might cause entities to be made available to following {@link EntityProcessor @EntityProcessor} methods in the same class only in the next {@link World#process()}.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface Archetype {
}
