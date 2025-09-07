package de.schosin.decs.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation similar to {@link javax.annotation.Generated} and {@link javax.annotation.processing.Generated} in newer JDK versions.
 * To support both JDK 8 and newer versions, a custom annotation is needed to identify generated types.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
public @interface Generated {

    /**
     * Returns the fully qualified name of the code generator.
     *
     * @return name of the code generator
     */
    String[] value();

    /**
     * Returns the ISO 8600 date when the annotated source was generated.
     *
     * @return date the source was generated
     */
    String date();

    /**
     * Returns additional comments by the code generator.
     *
     * @return comments by the code generator
     */
    String comments() default "";

}
