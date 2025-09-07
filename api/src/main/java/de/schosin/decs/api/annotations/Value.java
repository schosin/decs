package de.schosin.decs.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.exceptions.UnknownValueException;

/**
 * Annotation for injecting values into {@link EntityProcessor} and {@link SystemProcessor} methods.
 * 
 * <p>
 * These values can be set using {@link World#setValue(String, Object)} as well as the other overloads.
 * Using a value that has not been set will throw a {@link UnknownValueException}.
 * 
 * <p>
 * Supports usage as a meta-annotation on another annotation such as {@code @Delta}:
 * 
 * {@snippet:
 * @Target(ElementType.PARAMETER)
 * @Value("delta")
 * public @interface Delta {
 * }
 * }
 */
@Documented
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Value {

    /**
     * Name of the value that must be set using {@link World#setValue(String, Object)} 
     * or an overloaded value.
     */
    String value();

}
