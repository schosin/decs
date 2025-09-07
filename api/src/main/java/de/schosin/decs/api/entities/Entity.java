package de.schosin.decs.api.entities;

import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.utils.collections.EntityBag;

/**
 * This type represents an entity and can be declared as a parameter for methods annotated with {@link EntityProcessor}. <br />
 * Use this type only if you need to delete an entity, or transmute it and prefer just a plain {@code int} instead if only the id is needed.
 * 
 * <p>
 * <b>DO NOT</b> keep a reference to an instance of this type, it is only valid within the {@link EntityProcessor @EntityProcessor} method. <br />
 * If a reference to an entity is needed, use {@link #createRef()} to obtain a {@link EntityRef} instance of this entity. <br />
 * The returned value must either be {@link EntityRef#delete() deleted} or {@link EntityRef#free() freed} if no longer needed to avoid garbage collection. <br />
 * A {@link EntityBag} can be used to store a collection of references that are automatically removed if the referenced entity is deleted.
 */
public interface Entity {

    /**
     * Returns the id of the entity.
     */
    int id();

    /**
     * Marks the entity for deletion.
     */
    void delete();

    /**
     * Creates a reference to this entity that can be used outside of the method this {@link Entity} instance is obtained from.
     * 
     * <p>
     * The returned value must either be {@link EntityRef#delete() deleted} or {@link EntityRef#free() freed} if no longer needed to avoid garbage collection. <br />
     * A {@link EntityBag} can be used to store a collection of references that are automatically removed if the referenced entity is deleted.
     */
    EntityRef createRef();

}
