package de.schosin.decs.api.annotations.composition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits processing of entities to those having atleast one of the components declared by {@link #value()}.
 * 
 * <p>
 * When used on a type, it will apply to all entity processors that have no {@link de.schosin.decs.api.annotations.composition composition annotations}.
 * 
 * <p>
 * This annotation is repeatable. 
 * Every instance of {@link One @One} on a method will be checked independently. 
 * 
 * <p>
 * Can be combined with {@link All} and {@link None}.
 * Can be used on a meta-annotations, see {@link de.schosin.decs.api.annotations.composition}.
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
@Repeatable(Ones.class)
public @interface One {

    /**
     * Components that an entity must have atleast one of for this to match.
     * Must have atleast two values.
     */
    Class<?>[] value();

}
