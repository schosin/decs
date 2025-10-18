package de.schosin.decs.api.utils.collections;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.jspecify.annotations.NonNull;

/**
 * Collection type similar to {@link ArrayList} without ordering to improve performance of remove operations.
 * 
 * <p>
 * To iterate over this collection prefer a regular for loop over an enhanced for loop (iterator).
 * 
 * {@snippet:
 *     Object[] data = bag.getData();
 *     for (int i = 0, s = bag.getSize(); i < s; i++) {
 *         Object item = data[i];
 *     }
 * }
 * 
 * @param <T> type of element
 */
public class Bag<T> implements Iterable<T> {

    private final Class<? super T> elementClass;

    private T[] data;
    private int size;

    public Bag(Class<? super T> clazz) {
        this(clazz, 64);
    }

    @SuppressWarnings("unchecked")
    public Bag(Class<? super T> clazz, int arraySize) {
        this.elementClass = clazz;

        if (arraySize < 1) {
            throw new IllegalArgumentException("arraySize must be atleast 1");
        }

        this.data = (T[]) Array.newInstance(clazz, arraySize);
        this.size = 0;
    }

    public Class<? super T> getElementClass() {
        return this.elementClass;
    }

    public T[] getData() {
        return data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int add(@NonNull T item) {
        ensureCapacity(size);

        this.data[size++] = item;
        return size - 1;
    }

    public int addUnsafe(@NonNull T item) {
        this.data[size++] = item;
        return size - 1;
    }

    public void addAll(Bag<? extends T> other) {
        ensureCapacity(size + other.size());

        T[] data = other.data;
        for (int i = 0, s = other.size; i < s; i++) {
            add(data[i]);
        }
    }

    public T set(int index, T item) {
        if (index >= data.length) {
            ensureCapacity(index);
        }

        if (index >= size) {
            this.size = index + 1;
        }

        return this.data[index] = item;
    }

    public T setUnsafe(int index, T item) {
        if (index >= this.size) {
            this.size = index + 1;
        }

        return this.data[index] = item;
    }

    public T get(int index) {
        if (index >= data.length) {
            return null;
        }

        return data[index];
    }

    public T getUnsafe(int index) {
        return data[index];
    }

    public boolean contains(@NonNull T item) {
        return indexOf(item) > -1;
    }

    public int indexOf(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            if (data[i].equals(item)) {
                return i;
            }
        }

        return -1;
    }

    public T remove(int index) {
        T item = data[index];

        // move last item to cleared slot
        this.data[index] = data[--size];
        this.data[size] = null;

        return item;
    }

    public boolean remove(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            T test = data[i];

            if (item.equals(test)) {
                // move last item to cleared slot
                this.data[i] = data[--size];
                this.data[size] = null;

                return true;
            }
        }

        return false;
    }

    public boolean removeIdentity(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            T test = data[i];

            if (item == test) {
                // move last item to cleared slot
                this.data[i] = data[--size];
                this.data[size] = null;

                return true;
            }
        }

        return false;
    }

    public T removeLast() {
        T item = data[--size];
        this.data[size] = null;

        return item;
    }

    public void clear() {
        Arrays.fill(this.data, 0, this.size, null);
        this.size = 0;
    }

    public int getCapacity() {
        return this.data.length;
    }

    public void ensureCapacity(int index) {
        if (index >= data.length) {
            int newSize = Math.max(data.length * 2, index + 1);
            setCapacity(newSize);
        }
    }

    private void setCapacity(int length) {
        if (length > this.data.length) {
            synchronized (this) {
                if (length > this.data.length) {
                    this.data = Arrays.copyOf(data, length);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("Bag(");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(data[i]);
        }
        return builder.append(")").toString();
    }

    /**
     * Returns the iterator for this bag.
     * 
     * <p>
     * Do not use this method in performance critical code as it instantiates a new iterator for every call.
     * Use a regular for loop with {@link #size} and either {@link #getData()} or {@link #get(int)}.
     * 
     * {@snippet:
     *     Object[] data = bag.getData();
     *     for (int i = 0, s = bag.getSize(); i < s; i++) {
     *         Object item = data[i];
     *     }
     * }
     */
    @Override
    public Iterator<T> iterator() {
        return new BagIterator<>(this);
    }

    private static final class BagIterator<T> implements Iterator<T> {

        private final T[] data;
        private final int size;
        private int index;

        public BagIterator(Bag<T> bag) {
            this.data = bag.data;
            this.size = bag.size;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            return data[index++];
        }

    }

}
