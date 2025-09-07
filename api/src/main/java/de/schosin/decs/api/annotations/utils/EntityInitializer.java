package de.schosin.decs.api.annotations.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that initialize the components of entities. <br />
 * Can be used in conjunction with {@link Archetype @Archetype} and {@link Transmute @Transmute}.
 * 
 * <p>
 * The annotated method must immediately follow a {@link Archetype @Archetype} or {@link Transmute @Transmute} method. <br />
 * The method must have a {@code void} return type and must not be {@code private}. <br />
 * Depending on the corresponding annotation, the method must meet additional requirements, which are outlined below.
 * 
 * <h3>@Archetype</h3>
 * 
 * <p>
 * <ul>
 *  <li>
 *      The method must start with the same parameters (type and name) as its {@link Archetype @Archetype} method. <br />
 *      The first parameter of type {@code int} should be called {@code index} and will be the 0-based index of the entity and will increase to {@code count-1} of its {@link Archetype @Archetype} method. <br />
 *      These parameters will be called the "common prefix".
 *  <li>The method must declare atleast one component parameter after the common prefix.
 *  <li>The component parameters will be added to the created entities and this method will be called for each entity.
 * </ul>
 * 
 * <h3>@Transmute</h3>
 * 
 * <p>
 * <ul>
 *  <li>
 *      The method must start with the same parameters (type and name) as its {@link Transmute @Transmute} method. <br />
 *      The exception is the first parameter, which must be of type {@code int} and should be called {@code entityId}.
 *      These parameters will be called the "common prefix".
 *  <li>
 *      The method must declare atleast one component parameter after the common prefix. <br />
 *      The entities will have all components added they do not currently have, and this method will be called for each entity.
 *      If an entity already has a component, the existing instance will be passed to this method.
 * </ul>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface EntityInitializer {
}
