package de.schosin.decs.core.data;

import de.schosin.decs.api.exceptions.InvalidComponentException;
import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.api.utils.pool.Pool;
import de.schosin.decs.values.ComponentMetadata;
import de.schosin.decs.values.Components;

public final class ComponentIndex {

    private static final int[] EMPTY = new int[0];

    private final Bag<Pool<?>> pools = new Bag<>(Pool.class, 16);
    private final Bag<Class<?>> reverseLookup = new Bag<>(Class.class);

    private final ClassValue<Integer> lookup = new ClassValue<Integer>() {
        @Override
        protected Integer computeValue(Class<?> type) {
            ComponentMetadata<?> metadata = Components.getMetadata(type);
            if (metadata == null) {
                throw new InvalidComponentException(String.format("Metadata for component type '%s' not available.", type.getName()), type);
            }

            int id = metadata.getId();
            reverseLookup.set(id, type);
            pools.set(id, (Pool<?>) metadata.getPool());

            return id;
        }
    };

    public int getId(Class<?> component) {
        return lookup.get(component);
    }

    public Class<?> getComponent(int id) {
        return reverseLookup.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T> Pool<T> getPool(int componentId) {
        return (Pool<T>) this.pools.getUnsafe(componentId);
    }

    @SuppressWarnings("unchecked")
    public <T> Pool<T> getPool(Class<T> clazz) {
        int componentId = getId(clazz);
        return (Pool<T>) this.pools.getUnsafe(componentId);
    }

    public int[] sort(Class<?>[] components) {
        int s = components.length;
        if (s == 0) {
            return EMPTY;
        }

        // Create types
        int[] ids = new int[s];
        for (int i = 0; i < s; i++) {
            ids[i] = lookup.get(components[i]);
        }

        // Sort in-place
        for (int i = 0; i < s - 1; i++) {
            int minIndex = i;

            for (int j = i + 1; j < s; j++) {
                if (ids[j] < ids[minIndex]) {
                    minIndex = j;
                }
            }

            if (minIndex != i) {
                swap(components, i, minIndex);
                swap(ids, i, minIndex);
            }
        }

        return ids;
    }

    private static void swap(Object[] array, int oldIndex, int newIndex) {
        Object item = array[newIndex];
        array[newIndex] = array[oldIndex];
        array[oldIndex] = item;
    }

    private static void swap(int[] array, int oldIndex, int newIndex) {
        int item = array[newIndex];
        array[newIndex] = array[oldIndex];
        array[oldIndex] = item;
    }

}
