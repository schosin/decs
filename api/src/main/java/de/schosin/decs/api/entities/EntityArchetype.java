package de.schosin.decs.api.entities;

import java.util.List;

import org.jspecify.annotations.Nullable;

import de.schosin.decs.api.World;
import de.schosin.decs.api.exceptions.EntityDeletedException;
import de.schosin.decs.api.exceptions.EntityModifiedException;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.api.utils.collections.IntBag;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * An entity archetype is a container for all entities having the same component composition.
 * Whenever an entity is created, it is assigned to the archetype that matches the components given to the entity.
 * When a component is added or removed, the entity will be moved to another archetype matching the new component composition.
 * 
 * <p>
 * By grouping entities with the same composition together, iterating over them can be better optimized as the access patterns stays the same for all entities of an archetype.
 * Additionally {@link de.schosin.decs.api.annotations.composition composition annotations} only have to be matched against an archetype once, instead of against entities every time an entity is created or changed.
 * Additional cost occurs when an entity is changed (component added or removed), as the entity and its components have to be moved between archetypes.
 */
public interface EntityArchetype {

    /**
     * Returns the id of this archetype.
     * 
     * @return id of archetype
     */
    int id();

    /**
     * Returns the components of this archetype.
     * 
     * @return components of this archetype
     */
    List<Class<?>> getComponents();

    /**
     * Retrieves the bag of entities. 
     * The returned value will always be the same instance.
     * 
     * <p>
     * When iterating over the entities, use {@link #getAlive()} as the upper bound to avoid reading invalid data.
     * 
     * @return bag of entities
     */
    IntBag getEntities();

    /**
     * Returns the number of alive entities.
     * 
     * <p>
     * When iterating over {@link #getEntities() entities} use this count as the exclusive upper limit.
     * 
     * @return number of alive entities
     */
    int getAlive();

    /**
     * Retrieves the component data bag for the given component type.
     * 
     * @param <T> type of component
     * @param type class of component
     * @return bag of component data
     */
    <T> Bag<T> getData(Class<T> type);

    /**
     * Create a batch of entities and returns the index of the first entity in this archetype.
     * The caller must set component data for all indices starting at the return value up.
     * 
     * @param count number of entities to create
     * @return index of first entity
     */
    int createEntities(int count);

    /**
     * Returns the id of the entity at the given inex.
     * 
     * @param index index of entity
     * @return id of entity
     */
    int getEntityId(int index);

    /**
     * Marks the entity at the given index to be deleted.
     * The entity will be deleted during the next {@link World#process()} call.
     * 
     * @param index index of entity
     */
    void deleteEntity(int index);

    /**
     * Marks the entity at the given index to be moved to another archetype.
     * The entity will be removed from this archetype during the next {@link World#process()} call.
     * 
     * <p>
     * The caller is responsible to set the component data for enums in the {@link Transition#archetype() target archetype} at {@link Transition#index() the new index}.
     * 
     * @param index index of entity
     * @param transmutation transmutation to apply
     * @return transition containing target archetype and index
     * @throws EntityDeletedException if the entity in marked for deletion this call
     * @throws EntityModifiedException if the entity has already been moved this call
     */
    @Nullable
    Transition moveEntity(int index, Transmutation transmutation);

    /**
     * Creates and tracks a new {@link EntityRef} for the index.
     * 
     * @param index index to reference
     * @return reference instance
     */
    EntityRef createEntityRef(int index);

}
