package de.schosin.decs.api.annotations.invocation;

import de.schosin.decs.api.World;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Future;

/**
 * Declares a system group that can be used with {@link Systems}.
 * All system groups of a {@link Systems} will be run in sequence.
 *
 * <p>
 * A system group is a {@link #name() named} or unnamed group that runs its {@link #value() systems} according to the configured {@link #strategy() invocation strategy}.
 *
 * <p>
 * When declaring a named group by supplying a non-empty {@link #name()}, the system group can be {@link World#enableSystemGroup(String) enabled} and {@link World#disableSystemGroup(String) disabled} at runtime.
 * A named group can also be disabled at start by setting {@link #enabled()} to {@code false}.
 *
 * <p>
 * The default invocation strategy is {@link InvocationStrategy#SEQUENTIAL}, which will run all systems defined in {@link #value()} in sequence.
 * When using {@link InvocationStrategy#PARALLEL}, at least two systems must be defined.
 * These systems will be run in parallel.
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface SystemGroup {

    /**
     * The systems in this group.
     * Must only contain classes of types that are annotated with {@link de.schosin.decs.api.annotations.system system annotations}.
     *
     * <p>
     * When using {@link InvocationStrategy#PARALLEL} the slowest systems should be at the beginning.
     * This can avoid the cost of {@link Future#get() waiting for systems to finish}, as the later systems are more likely to have already finished.
     */
    Class<?>[] value();

    /**
     * Name of this system group.
     * The name must be unique for the {@link Systems @Systems}.
     *
     * <p>
     * Can be used to enable and disable those systems at runtime using {@link World#enableSystemGroup(String)} and  {@link World#disableSystemGroup(String)}.
     * If not defined or {@code ""}, the group is considered an unnamed group that cannot be enabled and disabled.
     *
     * <p>
     * <b>Attention:</b> Avoid using named groups if you do not need to disable them.
     * There is a runtime overhead when giving groups a name that unnamed groups do not have.
     */
    String name() default "";

    /**
     * Default state for this system group.
     *
     * <p>
     * Only {@link #name() named groups} can be disabled by setting this value to {@code false}.
     */
    boolean enabled() default true;

    /**
     * Invocation strategy for the systems in this group.
     * Defaults to sequential invocation.
     */
    InvocationStrategy strategy() default InvocationStrategy.SEQUENTIAL;

    enum InvocationStrategy {

        /**
         * This invocation strategy runs all systems sequentially in the order they were defined.
         */
        SEQUENTIAL,

        /**
         * This invocation strategy runs all systems in parallel.
         */
        PARALLEL

    }

}
