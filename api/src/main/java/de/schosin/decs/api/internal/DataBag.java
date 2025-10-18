package de.schosin.decs.api.internal;

import java.util.Arrays;

import de.schosin.decs.api.utils.collections.Bag;
import de.schosin.decs.api.utils.collections.FloatBag;
import de.schosin.decs.api.utils.collections.IntBag;
import de.schosin.decs.api.utils.pool.Pool;

interface DataBag {

    static <T> DataBag create(Bag<T> data, Pool<T> pool) {
        return new ObjectDataBag<>(data, pool);
    }

    static <T extends Enum<T>> DataBag create(Bag<T> data) {
        return new EnumDataBag<>(data);
    }

    static DataBag create(String name, IntBag data) {
        return new IntDataBag(name, data);
    }

    static DataBag create(String name, FloatBag data) {
        return new FloatDataBag(name, data);
    }

    static DataBag create(DataBag... bags) {
        return new CompositeDataBag(bags);
    }

    void ensureCapacity(int index);

    void removeLast();

    void remove(int index);

    void clear(int index);

    void copyTo(int index, DataBag target, int targetIndex);

    void validateState(int expectedSize);

}

final class ObjectDataBag<T> implements DataBag {

    private final Bag<T> data;
    private final Pool<T> pool;

    ObjectDataBag(Bag<T> data, Pool<T> pool) {
        this.data = data;
        this.pool = pool;
    }

    @Override
    public void ensureCapacity(int index) {
        this.data.ensureCapacity(index);
    }

    void initialize(int index) {
        this.data.set(index, pool.getInstance());
    }

    @Override
    public void removeLast() {
        this.data.removeLast();
    }

    void removeAndFreeLast() {
        T component = this.data.removeLast();
        this.pool.free(component);
    }

    @Override
    public void remove(int index) {
        this.data.set(index, this.data.removeLast());
    }

    void removeAndFree(int index) {
        T component = this.data.get(index);
        this.pool.free(component);

        this.data.set(index, this.data.removeLast());
    }

    @Override
    public void clear(int index) {
        this.data.set(index, null);
    }

    void clearAndFree(int index) {
        T component = this.data.get(index);
        this.pool.free(component);

        this.data.set(index, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void copyTo(int index, DataBag target, int targetIndex) {
        ((ObjectDataBag<T>) target).data.setUnsafe(targetIndex, this.data.getUnsafe(index));
    }

    @Override
    public void validateState(int expectedSize) {
        String name = this.data.getElementClass().getSimpleName();

        if (data.size() != expectedSize) {
            throw new IllegalStateException(String.format("Data for component %s has size %d, expected %d: %s", name, data.size(), expectedSize, data));
        }

        T[] data = this.data.getData();
        for (int i = 0, s = this.data.size(); i < s; i++) {
            if (data[i] == null) {
                throw new IllegalStateException(String.format("Data for component %s contains null at index %d: %s", name, i, data));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ObjectDataBag [data=").append(this.data).append(", pool=").append(this.pool).append("]");
        return builder.toString();
    }

}

final class EnumDataBag<T extends Enum<T>> implements DataBag {

    private final Bag<T> data;

    EnumDataBag(Bag<T> data) {
        this.data = data;
    }

    @Override
    public void ensureCapacity(int index) {
        this.data.ensureCapacity(index);
    }

    @Override
    public void removeLast() {
        this.data.removeLast();
    }

    @Override
    public void remove(int index) {
        this.data.set(index, this.data.removeLast());
    }

    @Override
    public void clear(int index) {
        this.data.set(index, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void copyTo(int index, DataBag target, int targetIndex) {
        ((EnumDataBag<T>) target).data.setUnsafe(targetIndex, this.data.getUnsafe(index));
    }

    @Override
    public void validateState(int expectedSize) {
        String name = this.data.getElementClass().getSimpleName();

        if (data.size() != expectedSize) {
            throw new IllegalStateException(String.format("Data for component %s has size %d, expected %d: %s", name, data.size(), expectedSize, data));
        }

        T[] data = this.data.getData();
        for (int i = 0, s = this.data.size(); i < s; i++) {
            if (data[i] == null) {
                throw new IllegalStateException(String.format("Data for component %s contains null at index %d: %s", name, i, data));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EnumDataBag [data=").append(this.data).append("]");
        return builder.toString();
    }

}

final class IntDataBag implements DataBag {

    private final String name;
    private final IntBag data;

    IntDataBag(String name, IntBag data) {
        this.name = name;
        this.data = data;
    }

    @Override
    public void ensureCapacity(int index) {
        this.data.ensureCapacity(index);
    }

    @Override
    public void removeLast() {
        this.data.removeLast();
    }

    @Override
    public void remove(int index) {
        this.data.set(index, this.data.removeLast());
    }

    @Override
    public void clear(int index) {
        this.data.set(index, 0);
    }

    @Override
    public void copyTo(int index, DataBag target, int targetIndex) {
        ((IntDataBag) target).data.setUnsafe(targetIndex, this.data.getUnsafe(index));
    }

    @Override
    public void validateState(int expectedSize) {
        if (data.size() != expectedSize) {
            throw new IllegalStateException(String.format("Data for component %s has size %d, expected %d: %s", name, data.size(), expectedSize, data));
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IntDataBag [name=").append(this.name).append(", data=").append(this.data).append("]");
        return builder.toString();
    }

}

final class FloatDataBag implements DataBag {

    private final String name;
    private final FloatBag data;

    FloatDataBag(String name, FloatBag data) {
        this.name = name;
        this.data = data;
    }

    @Override
    public void ensureCapacity(int index) {
        this.data.ensureCapacity(index);
    }

    @Override
    public void removeLast() {
        this.data.removeLast();
    }

    @Override
    public void remove(int index) {
        this.data.set(index, this.data.removeLast());
    }

    @Override
    public void clear(int index) {
        this.data.set(index, 0);
    }

    @Override
    public void copyTo(int index, DataBag target, int targetIndex) {
        ((FloatDataBag) target).data.setUnsafe(targetIndex, this.data.getUnsafe(index));
    }

    @Override
    public void validateState(int expectedSize) {
        if (data.size() != expectedSize) {
            throw new IllegalStateException(String.format("Data for component %s has size %d, expected %d: %s", name, data.size(), expectedSize, data));
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IntDataBag [name=").append(this.name).append(", data=").append(this.data).append("]");
        return builder.toString();
    }

}

final class CompositeDataBag implements DataBag {

    private final DataBag[] bags;
    private final int size;

    CompositeDataBag(DataBag[] bags) {
        this.bags = bags;
        this.size = bags.length;
    }

    @Override
    public void ensureCapacity(int index) {
        for (int i = 0; i < size; i++) {
            this.bags[i].ensureCapacity(index);
        }
    }

    @Override
    public void removeLast() {
        for (int i = 0; i < size; i++) {
            this.bags[i].removeLast();
        }
    }

    @Override
    public void remove(int index) {
        for (int i = 0; i < size; i++) {
            this.bags[i].remove(index);
        }
    }

    @Override
    public void clear(int index) {
        for (int i = 0; i < size; i++) {
            this.bags[i].clear(index);
        }
    }

    @Override
    public void copyTo(int index, DataBag target, int targetIndex) {
        for (int i = 0; i < size; i++) {
            this.bags[i].copyTo(index, target, targetIndex);
        }
    }

    @Override
    public void validateState(int expectedSize) {
        for (int i = 0; i < size; i++) {
            this.bags[i].validateState(expectedSize);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CompositeDataBag [bags=").append(Arrays.toString(this.bags)).append("]");
        return builder.toString();
    }

}
