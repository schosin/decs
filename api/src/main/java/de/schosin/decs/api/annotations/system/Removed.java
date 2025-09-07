package de.schosin.decs.api.annotations.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.composition.One;

/**
 * Annotation for declaring methods as callbacks for whenever an entity no longer matches the methods component composition.
 * 
 * <p>
 * The annotated method is called when an entity is deleted that matches the component composition.
 * The annotated method is called when an entity is modified (components added or removed) such that it no longer matches the component composition afterwards. 
 * 
 * <p>
 * Component compositions are declared using a combination of {@link All @All}, {@link One @One} and {@link None @None}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Removed {
}
