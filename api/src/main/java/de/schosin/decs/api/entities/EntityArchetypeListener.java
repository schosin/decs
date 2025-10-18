package de.schosin.decs.api.entities;

import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.utils.collections.IntBag;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Listener interface for {@link EntityArchetype#inserted(EntityArchetypeListener)} and {@link EntityArchetype#removed(EntityArchetypeListener)}.
 * Used by the code generation to implement {@link Inserted @Inserted} and {@link Removed @Removed} methods.
 */
public interface EntityArchetypeListener {

    /**
     * Callback method for when entities are inserted or removed.
     * To process the entities, iterate them from {@code start} to {@code end} on the same archetype.
     * 
     * @param start start index, inclusive
     * @param end end index, exclusive
     */
    void process(int start, int end);

    /**
     * Callback method for when entities are inserted or removed.
     * To process the entities, iterate over the {@link IntBag} {@code indices}.
     * 
     * @param indices indices of entities
     */
    void process(IntBag indices);

    default void processEntity(int index, int entityId) {
        // TODO benchmark whether this or the IntBag approach is better (this variant would eliminate one for loop over the indices, but IntBag might have better access patterns) 
    }

}
