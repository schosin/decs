package de.schosin.decs.api.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.utils.pool.Pooled;

/**
 * Declares a type as a component.
 * Supported types are {@code class} and {@code enum}.
 * 
 * <h3>Class components</h3>
 * 
 * <p>
 * Class components are the main type of components and are intended as mutable data holders for entities. <br />
 * Class components must be {@code public final} and require a public default constructor.
 * 
 * <p>
 * Component instances are pooled by the library to avoid garbage collection and runtime allocation. <br />
 * Class components may implement {@link Pooled} to run simple clean up logic whenever an instance is not in use anymore. <br />
 * For more complex clean up logic, use a {@link Removed @Removed method} instead, which supports clean up logic across multiple components. 
 * 
 * <h3>Enum components</h3>
 * 
 * <p>
 * Enum components should be used when a component does not carry any data other than the instance used. <br />
 * Enums with only a single instance can be further optimized by avoiding storing them altogether, improving memory usage and access patterns.
 */
@Target( ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Component {
}
