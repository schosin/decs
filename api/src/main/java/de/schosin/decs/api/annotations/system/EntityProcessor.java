package de.schosin.decs.api.annotations.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.World;
import de.schosin.decs.api.builder.WorldBuilder;

/**
 * TODO
 * 
 * <p>
 * Annotated methods can declare any combination of the following arguments.
 * Arguments must not start with underscore (_).
 * 
 * <p>
 * <table border="1px">
 *  <thead>
 *  <tr>
 *      <th>Type
 *      <th>Description
 *  </tr>
 *  </thead>
 *  <tbody>
 *  <tr>
 *      <td>int
 *      <td>Id of entity
 *  </tr>
 *  <tr>
 *      <td>&lt;type&gt;
 *      <td>Component
 *  </tr>
 *  <tr>
 *      <td>Transmuter&lt;T&gt;
 *      <td>Transmuter for adding or removing a component
 *  </tr>
 *  <tr>
 *      <td>World
 *      <td>Instance of the current world
 *  </tr>
 *  <tr>
 *      <td>@Singleton &lt;type&gt;
 *      <td>Singleton set with {@link WorldBuilder#singleton(Object)}
 *  </tr>
 *  <tr>
 *      <td>@Value("...") int
 *      <td>Value set with {@link World#setValue(String, int)} 
 *  </tr>
 *  <tr>
 *      <td>@Value("...") long
 *      <td>Value set with {@link World#setValue(String, long)} 
 *  </tr>
 *  <tr>
 *      <td>@Value("...") float
 *      <td>Value set with {@link World#setValue(String, float)} 
 *  </tr>
 *  <tr>
 *      <td>@Value("...") double
 *      <td>Value set with {@link World#setValue(String, double)} 
 *  </tr>
 *  <tr>
 *      <td>@Value("...") &lt;type&gt;
 *      <td>Value set with {@link World#setValue(String, Object)} 
 *  </tr>
 *  </tbody>
 * </table>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface EntityProcessor {

    /**
     * <b>EXPERIMENTAL:</b>
     *
     * <p>
     * Configures the invocation strategy of the archetypes that are processed by this method. <br />
     * <b>Only supports read-only workloads while concurrent writes are not fully implemented yet.</b>
     */
    ParallelStrategy parallel() default ParallelStrategy.NONE;

    enum ParallelStrategy {

        /**
         * Generated code runs all entities sequentially.
         */
        NONE,

        /**
         * Generated code runs all archetypes in its own thread.
         *
         * <p>
         * This strategy works best when the entities are spread across multiple archetypes and the archetypes are comparable in their number of entities.
         *
         */
        PER_ARCHETYPE;

    }

}
