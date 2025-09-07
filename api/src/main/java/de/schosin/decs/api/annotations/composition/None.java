package de.schosin.decs.api.annotations.composition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits processing of entities to those having none of the components declared by {@link #value()}.
 * 
 * <p>
 * When used on a type, it will apply to all entity processors that have no {@link de.schosin.decs.api.annotations.composition composition annotations}.
 * 
 * <p>
 * Can be combined with {@link All} and {@link One}.
 * Can be used on a meta-annotations, see {@link de.schosin.decs.api.annotations.composition}.
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface None {

    /**
     * Components that an entity must not have for this to match.
     */
    Class<?>[] value();

}
