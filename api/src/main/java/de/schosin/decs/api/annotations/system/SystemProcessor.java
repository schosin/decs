package de.schosin.decs.api.annotations.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.World;

/**
 * TODO
 * 
 * <p>
 * Annotated methods can declare any combination of the following arguments:
 * 
 * <table border="1px">
 *  <thead>
 *  <tr>
 *      <th>Type
 *      <th>Description
 *  </tr>
 *  </thead>
 *  <tbody>
 *  <tr>
 *      <td>World
 *      <td>Instance of the current world
 *  </tr>
 *  </tbody>
 * </table>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface SystemProcessor {
    
    /**
     * Set this value to true if the method may create, delete, or transmute entities.
     * Use this if subsequent {@link EntityProcessor @EntityProcessor} methods in the same type need to see these entities this frame.
     * 
     * <p>
     * Alternatively use {@link World#fl}
     * 
     * <p>
     * Changes are flushed automatically in between system types, so setting this to true is only required for {@link EntityProcessor @EntityProcessor} methods in this type.
     */
    boolean modifying() default false;
    
}
