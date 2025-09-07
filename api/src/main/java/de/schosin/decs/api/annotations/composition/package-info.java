/**
 * This package contains the annotations for component compositions.
 * These annotations can be used on methods annotated with {@link de.schosin.decs.api.annotations.system.EntityProcessor @EntityProcessor}, {@link de.schosin.decs.api.annotations.system.Inserted @Inserted} or {@link de.schosin.decs.api.annotations.system.Removed @Removed}.
 * Combine the annotations in this package to declare which entities should be processed by the annotated methods.
 * 
 * <h3>Keeping track of entities</h3>
 * 
 * When keeping references to entities, use {@code @Inserted} and {@code @Removed} with the same set of composition annotations.
 * To avoid them drifting apart, annotations in this package can be used as meta-annotations.
 * 
 * {@snippet:
 * @All({ Position.class, Velocity.class })
 * @None(Acceleration.class)
 * public @interface PhysicsComposition {
 * }}
 * 
 * This annotation can then be used in a system to keep track of entities.
 * 
 * {@snippet:
 * public class PhysicsSystem {
 *     
 *     private final IntBag entities = new IntBag();
 * 
 *     @Inserted
 *     @PhysicsComposition
 *     public final void inserted(int entityId) {
 *         this.entities.add(entityId);
 *     }
 * 
 *     @Removed
 *     @PhysicsComposition
 *     public final void inserted(int entityId) {
 *         this.entities.removeValue(entityId);
 *     }
 * }
 * }
 */
package de.schosin.decs.api.annotations.composition;
