package de.schosin.decs.api.utils.collections;

import java.util.Arrays;
import java.util.PrimitiveIterator;

/**
 * Collection over primitive ints without ordering to improve performance of remove operations.
 * 
 * <p>
 * To iterate over this collection prefer a regular for loop over an enhanced for loop (iterator).
 * 
 * {@snippet:
 *     int[] data = bag.getData();
 *     for (int i = 0, s = bag.getSize(); i < s; i++) {
 *         int value = data[i];
 *     }
 * }
 */
public class IntBag {

    private int[] data;
    private int size;

    public IntBag() {
        this(64);
    }

    public IntBag(int arraySize) {
        if (arraySize < 1) {
            throw new IllegalArgumentException("arraySize must be atleast 1");
        }

        this.data = new int[arraySize];
        this.size = 0;
    }

    public IntBag(int[] array) {
        int arraySize = array.length;
        if (array.length == 0) {
            this.data = new int[64];
            this.size = 0;
        } else {
            this.data = Arrays.copyOf(array, arraySize);
            this.size = arraySize;
        }
    }

    public int[] getData() {
        return data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int add(int value) {
        ensureCapacity(size);

        this.data[size++] = value;
        return size - 1;
    }

    public int addSafe(int value) {
        this.data[size++] = value;
        return size - 1;
    }

    public void insert(int index, int value) {
        ensureCapacity(size);

        if (index >= this.size) {
            this.size = index + 1;
        }

        for (int i = size - 1; i >= index; i--) {
            this.data[i + 1] = this.data[i];
        }

        this.data[index] = value;
    }

    public void set(int index, int value) {
        ensureCapacity(index);

        if (index >= this.size) {
            this.size = index + 1;
        }

        this.data[index] = value;
    }

    public void setUnsafe(int index, int value) {
        if (index >= this.size) {
            this.size = index + 1;
        }

        this.data[index] = value;
    }

    public int get(int index) {
        if (index >= data.length) {
            return 0;
        }

        return data[index];
    }

    public int getUnsafe(int index) {
        return data[index];
    }

    public int indexOf(int value) {
        for (int i = 0; i < size; i++) {
            if (value == data[i]) {
                return i;
            }
        }

        return -1;
    }

    public int removeIndex(int index) {
        int value = data[index];

        // move last value to cleared slot
        this.data[index] = data[--size];
        this.data[size] = 0;

        return value;
    }

    public int removeValue(int value) {
        int index = indexOf(value);
        if (index > -1) {
            removeIndex(index);
            return index;
        }

        return -1;
    }

    public int removeLast() {
        int value = data[--size];
        this.data[size] = 0;

        return value;
    }

    public void clear() {
        Arrays.fill(this.data, 0, this.size, 0);
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
    public int hashCode() {
        int result = 1;
        for (int i = 0, s = this.size; i < s; i++) {
            result = 31 * result + this.data[i];
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IntBag)) {
            return false;
        }

        IntBag other = (IntBag) obj;
        if (this.size != other.size()) {
            return false;
        }

        for (int i = 0, s = this.size; i < s; i++) {
            if (this.data[i] != other.get(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("IntBag(");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(data[i]);
        }
        return builder.append(")").toString();
    }

    /**
     * Returns a primitive iterator for this bag.
     * 
     * <p>
     * Do not use this method in performance critical code as it instantiates a new iterator for every call.
     * Use a regular for loop with {@link #size} and either {@link #getData()} or {@link #get(int)}.
     * 
     * {@snippet:
     *     int[] data = bag.getData();
     *     for (int i = 0, s = bag.getSize(); i < s; i++) {
     *         int value = data[i];
     *     }
     * }
     */
    public PrimitiveIterator.OfInt iterator() {
        return new IntBagIterator(this);
    }

    private static final class IntBagIterator implements PrimitiveIterator.OfInt {

        private final int[] data;
        private final int size;
        private int index;

        public IntBagIterator(IntBag bag) {
            this.data = bag.data;
            this.size = bag.size;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public int nextInt() {
            return data[index++];
        }

    }

}
