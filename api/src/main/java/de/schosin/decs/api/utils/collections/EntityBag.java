package de.schosin.decs.api.utils.collections;

import org.jspecify.annotations.NonNull;

import de.schosin.decs.api.World;
import de.schosin.decs.api.entities.EntityRef;

/**
 * Specialized collection for {@link EntityRef} elements. <br />
 * This type has {@link Bag} semantics, that means insertion order is preserved until the first element is removed. <br />
 * Removing elements from the bag will call {@link EntityRef#free()} on those, invalidating them.
 * 
 * <p>
 * For iteration, prefer a regular for loop using {@link #size()} and {@link #get(int)} over {@link #iterator()} to avoid memory allocations.
 * 
 * <p>
 * An instance can be obtained with {@link World#createEntityBag()}. <br />
 * Instances should be {@link #free() freed} when no longer needed to avoid garbage collection.
 */
public interface EntityBag extends Iterable<EntityRef> {

    int size();

    boolean isEmpty();

    void add(@NonNull EntityRef entity);

    void addAll(EntityBag other);

    boolean contains(EntityRef entity);

    EntityRef get(int index);

    EntityRef getUnsafe(int index);

    void remove(int index);

    void removeUnsafe(int index);

    void removeAll(EntityRef entity);

    void removeAll(EntityBag other);

    void clear();

    /**
     * Calls {@link EntityRef#free()} on all contained elements and frees this bag for reuse.
     */
    void free();

}
