package de.schosin.decs.api.annotations.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used on abstract methods to retrieve the number of entities matching a {@link de.schosin.decs.api.annotations.composition composition}.
 * 
 * <p>
 * The method must be annotated with atleast one {@link de.schosin.decs.api.annotations.composition composition annotation}. <br />
 * The method must not have any parameters. <br />
 * The method must have an {@code int} return type.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface Count {
}
