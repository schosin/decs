package de.schosin.decs.api.annotations.invocation;

import de.schosin.decs.api.World;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the invocation configuration.
 *
 * <p>
 * This annotation must be used on interface types extending {@link de.schosin.decs.api.systems.SystemInvocation}.
 * These interfaces must not declare abstract methods.
 *
 * <p>
 * The annotation processor will generate an implementation with an optimized {@link World#process()} based on the {@link #value() declared systems}. <br />
 * Additionally the annotation processor will analyze the systems and emit compiler errors when potential race conditions can occur in parallel systems.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Systems {

    /**
     * Declares the systems to run as different, optionally named system groups.
     *
     * <p>
     * Each step is run sequentially when {@link World#process()} is called. <br />
     * Systems run with {@link SystemGroup.InvocationStrategy#SEQUENTIAL} will be run in sequence. <br />
     * Systems run with {@link SystemGroup.InvocationStrategy#PARALLEL} will be run in parallel. <br />
     */
    SystemGroup[] value();

}
