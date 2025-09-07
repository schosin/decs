package de.schosin.decs.api.entities;

import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.utils.collections.EntityBag;

/**
 * This type represents a reference to an entity and can be declared as a parameter for methods annotated with {@link EntityProcessor}.
 * 
 * <p>
 * Use this type only if you need to delete an entity, or transmute it outside of its method and prefer {@link Entity} otherwise. <br />
 * When keeping a collection of references, consider using {@link EntityBag}.
 * That type automatically handles deleted entities and frees the references when removed from the collection.
 * 
 * <p>
 * <b>ATTENTION:</b> <br /> 
 * This type should not be shared, use {@link #copy()} to create a copy instead! <br />
 * Make sure to call call {@link #free()} if the reference is no longer needed.
 * Read the documentation of APIs accepting or working on this type to know whether they already free the references themselves, such as {@link EntityBag}.
 */
public interface EntityRef {

    /**
     * Returns the id of the entity.
     * 
     * <p>
     * Will return {@code -1} if this entity is no longer {@link #valid() valid}. <br />
     * Invalid references should be {@link #free() freed} to avoid garbage collection.
     */
    int id();

    /**
     * Marks the entity for deletion and invalidates this instance.
     * 
     * <p>
     * The reference to this instance must be removed. <br />
     * Calling {@link #free()} is not needed.
     */
    void delete();

    /**
     * Gets the component assigned to this entity. <br />
     * 
     * @return component or {@code null} if the entity does not have that component or this instance is not {@link #valid() valid}
     */
    <T> T getComponent(Class<T> clazz);

    /**
     * Returns whether this reference is still valid. <br />
     * {@link EntityRef} is invalidated when the referenced entity got deleted.
     */
    boolean valid();
    
    /**
     * Returns a copy of this instance.
     * 
     * @return new copy
     * @throws IllegalStateException when this instance is not {@link #valid() valid}.
     */
    EntityRef copy();

    /**
     * Frees the reference to be reused.
     */
    void free();

}
