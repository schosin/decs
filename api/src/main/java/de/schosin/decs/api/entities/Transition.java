package de.schosin.decs.api.entities;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Result type for {@link EntityArchetype#moveEntity(int, Transmutation)}.
 * Contains the target archetype as well as the new index in this archetype.
 * 
 * <p>
 * This type implements {@link AutoCloseable} and must be closed to garbage collection.
 */
public interface Transition {

    /**
     * Returns the target archetype of the transition.
     * 
     * @return target archetype
     */
    EntityArchetype archetype();

    /**
     * Returns the index in the target archetype.
     * 
     * @return index in target archetype
     */
    int index();

    /**
     * Frees this instance and returns it to its pool.
     */
    void free();

}
