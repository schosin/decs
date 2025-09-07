package de.schosin.decs.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.builder.WorldBuilder;

/**
 * Annotation for injecting singletons into {@link EntityProcessor} and {@link SystemProcessor} methods.
 * 
 * <p>
 * Singletons can be set with {@link WorldBuilder#singleton(Object)}.
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.CLASS)
public @interface Singleton {
}
