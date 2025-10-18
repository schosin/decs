package de.schosin.decs.api.internal;

import java.util.Arrays;
import java.util.Objects;

import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.utils.collections.Bag;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Base class for its generated implementation holding the component data for the entities of an {@link EntityArchetype}.
 */
public abstract class EntityArchetypeData {

    private final DataBag[] data;
    private final DataBag[] nonNullData;

    private final DataBag[] unpooled;
    @SuppressWarnings("rawtypes")
    private final ObjectDataBag[] pooled;

    EntityArchetypeData(DataBag[] data) {
        this.data = data;
        this.nonNullData = Arrays.stream(data).filter(Objects::nonNull).toArray(DataBag[]::new);

        this.unpooled = Arrays.stream(data)
                .filter(bag -> bag != null && !(bag instanceof ObjectDataBag))
                .toArray(DataBag[]::new);

        this.pooled = Arrays.stream(data)
                .filter(ObjectDataBag.class::isInstance)
                .map(ObjectDataBag.class::cast)
                .toArray(ObjectDataBag[]::new);
    }

    /**
     * For debugging purposes during development, will be removed / changed to a opt-in later.
     */
    public void validateState(int expectedSize) {
        for (int i = 0, s = this.nonNullData.length; i < s; i++) {
            this.nonNullData[i].validateState(expectedSize);
        }
    }

    @Deprecated
    public <T> Bag<T> getData(Class<T> type) {
        throw new UnsupportedOperationException(String.format("getData(%s) not supported", type.getName()));
    }

    public final void ensureCapacity(int index) {
        for (int i = 0, s = this.nonNullData.length; i < s; i++) {
            this.nonNullData[i].ensureCapacity(index);
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
            this.pooled[i].initialize(index);
        }
    }

    /**
     * Removes the components of the entity at the last index.
     */
    public final void removeLastComponents() {
        for (int i = 0, s = this.nonNullData.length; i < s; i++) {
            this.nonNullData[i].removeLast();
        }
    }

    /**
     * Removes the components of the entity at the given index. <br />
     * The components of the last entity will be moved to {@code index}.
     * 
     * @param index index of entity
     */
    public final void removeComponents(int index) {
        for (int i = 0, s = this.nonNullData.length; i < s; i++) {
            this.nonNullData[i].remove(index);
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
            this.pooled[i].removeAndFreeLast();
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
            this.unpooled[i].remove(index);
        }

        for (int i = 0, s = this.pooled.length; i < s; i++) {
            this.pooled[i].removeAndFree(index);
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
                ((ObjectDataBag<?>) this.data[i]).clearAndFree(index);
            } else {
                DataBag data = this.data[i];
                if (data != null) {
                    data.clear(index);
                }
            }
        }
    }

    /**
     * Copies entity data from this instance to the target instance.
     * 
     * @param sourceIndex index in this instance
     * @param targetData target instance
     * @param targetIndex index in target instance
     * @param mapping mapping matching this data in length to the target component index
     * @param create indices of components that need initialization (not in source archetype)
     */
    public void moveEntity(int sourceIndex, EntityArchetypeData targetData, int targetIndex, int[] mapping, int[] create) {
        // Copy source components
        for (int i = 0, s = mapping.length; i < s; i++) {
            int componentIndex = mapping[i];
            if (componentIndex > -1) {
                this.data[i].copyTo(sourceIndex, targetData.data[componentIndex], targetIndex);
            }
        }

        // Add new components from pool
        for (int i = 0, s = create.length; i < s; i++) {
            int componentIndex = create[i];
            ((ObjectDataBag<?>) targetData.data[componentIndex]).initialize(targetIndex);
        }

    }

}
