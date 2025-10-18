package de.schosin.decs.api.internal;

import java.util.List;
import java.util.stream.IntStream;

import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.api.utils.pool.Pool;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Base class for its generated implementation holding the component data for the entities of an {@link EntityArchetype}.
 */
public abstract class EntityArchetypeData {

    private final Bag<Object>[] data;
    private final Pool<Object>[] pools;

    private final Bag<Object>[] unpooled;
    private final int[] pooled;

    EntityArchetypeData(Bag<Object>[] data, Pool<Object>[] pools) {
        this.data = data;
        this.pools = pools;

        this.unpooled = IntStream.range(0, pools.length)
                .filter(i -> pools[i] == null)
                .mapToObj(i -> data[i])
                .filter(bag -> bag != null)
                .<Bag<Object>>toArray(Bag[]::new);

        this.pooled = IntStream.range(0, pools.length)
                .filter(i -> pools[i] != null)
                .toArray();
    }

    public final Bag<Object>[] getData() {
        return this.data;
    }

    public final Pool<Object>[] getPools() {
        return this.pools;
    }

    public final void ensureCapacity(int index) {
        for (int i = 0, s = this.data.length; i < s; i++) {
            this.data[i].ensureCapacity(index);
        }
    }

    /**
     * Initializes the components for the entity at the given index.
     * Assumes {@link #ensureCapacity(int)} has been called beforehand.
     * 
     * @param index index of entity
     */
    public final void initialize(int index) {
        for (int i = 0, s = this.pooled.length; i < s; i++) {
            int componentIndex = this.pooled[i];
            this.data[componentIndex].setUnsafe(index, this.pools[componentIndex].getInstance());
        }
    }

    /**
     * Removes the components of the entity at the last index.
     */
    public final void removeLastComponents() {
        for (int i = 0, s = this.data.length; i < s; i++) {
            this.data[i].removeLast();
        }
    }

    /**
     * Removes the components of the entity at the given index. <br />
     * The components of the last entity will be moved to {@code index}.
     * 
     * @param index index of entity
     */
    public final void removeComponents(int index) {
        for (int i = 0, s = this.data.length; i < s; i++) {
            Bag<Object> components = this.data[i];
            components.set(index, components.removeLast());
        }
    }

    /**
     * Removes the components of the entity at the last index, freeing them for reuse.
     */
    public final void removeAndFreeLastComponents() {
        for (int i = 0, s = this.unpooled.length; i < s; i++) {
            this.unpooled[i].removeLast();
        }

        for (int i = 0, s = this.pooled.length; i < s; i++) {
            int componentIndex = this.pooled[i];

            Object component = this.data[componentIndex].removeLast();
            this.pools[componentIndex].free(component);
        }
    }

    /**
     * Removes the components of the entity at the given index, freeing them for reuse. <br />
     * The components of the last entity will be moved to {@code index}.
     * 
     * @param index index of entity
     */
    public final void removeAndFreeComponents(int index) {
        for (int i = 0, s = this.unpooled.length; i < s; i++) {
            Bag<Object> components = this.unpooled[i];
            components.set(index, components.removeLast());
        }

        for (int i = 0, s = this.pooled.length; i < s; i++) {
            int componentIndex = this.pooled[i];
            Bag<Object> components = this.data[componentIndex];

            Object component = components.get(index);
            this.pools[componentIndex].free(component);

            components.set(index, components.removeLast());
        }
    }

    /**
     * Removes the components of the entity at the given index, freeing components according to {@code free}.
     * 
     * @param index index of entity
     * @param free components to free, indexed by their index in {@link #data}
     */
    public final void removeComponents(int index, boolean[] free) {
        for (int i = 0, s = this.data.length; i < s; i++) {
            if (free[i]) {
                Object component = this.data[i].set(index, null);
                this.pools[i].free(component);

            } else {
                this.data[i].set(index, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Bag<T> createBag(Class<T> component, List<Class<?>> components, int size, Bag<Bag<Object>> bags) {
        if (!components.contains(component)) {
            return null;
        }

        Bag<T> result = new Bag<>(component, size);
        bags.add((Bag<Object>) result);

        return result;
    }

}
