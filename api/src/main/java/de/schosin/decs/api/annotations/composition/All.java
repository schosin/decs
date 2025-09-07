package de.schosin.decs.api.annotations.composition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits processing of entities to those having all of the components declared by {@link #value()}.
 * 
 * <p>
 * When used on a type, it will apply to all entity processors that have no {@link de.schosin.decs.api.annotations.composition composition annotations}.
 * 
 * <p>
 * Can be combined with {@link One} and {@link None}.
 * Can be used on a meta-annotations, see {@link de.schosin.decs.api.annotations.composition}.
 * When used without {@link One} and {@link None} and no {@link #value()} is specified, targeted entity processors will match all entities.
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface All {

    /**
     * Components that an entity must have for this to match.
     * When left empty all entities will be matched.
     */
    Class<?>[] value() default {};

}
