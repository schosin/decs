package de.schosin.decs.api.annotations.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
}
