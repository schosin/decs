package de.schosin.decs.api.utils.collections;

import java.util.Arrays;

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
public class FloatBag {

    private float[] data;
    private int size;

    public FloatBag() {
        this(64);
    }

    public FloatBag(int arraySize) {
        if (arraySize < 1) {
            throw new IllegalArgumentException("arraySize must be atleast 1");
        }

        this.data = new float[arraySize];
        this.size = 0;
    }

    public FloatBag(float[] array) {
        int arraySize = array.length;
        if (array.length == 0) {
            this.data = new float[64];
            this.size = 0;
        } else {
            this.data = Arrays.copyOf(array, arraySize);
            this.size = arraySize;
        }
    }

    public float[] getData() {
        return data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int add(float value) {
        ensureCapacity(size);

        this.data[size++] = value;
        return size - 1;
    }

    public int addSafe(float value) {
        this.data[size++] = value;
        return size - 1;
    }

    public void insert(int index, float value) {
        ensureCapacity(size);

        if (index >= this.size) {
            this.size = index + 1;
        }

        for (int i = size - 1; i >= index; i--) {
            this.data[i + 1] = this.data[i];
        }

        this.data[index] = value;
    }

    public void set(int index, float value) {
        ensureCapacity(index);

        if (index >= this.size) {
            this.size = index + 1;
        }

        this.data[index] = value;
    }

    public void setUnsafe(int index, float value) {
        if (index >= this.size) {
            this.size = index + 1;
        }

        this.data[index] = value;
    }

    public float get(int index) {
        if (index >= data.length) {
            return 0f;
        }

        return data[index];
    }

    public float getUnsafe(int index) {
        return data[index];
    }

    public int indexOf(float value) {
        for (int i = 0; i < size; i++) {
            if (value == data[i]) {
                return i;
            }
        }

        return -1;
    }

    public float removeIndex(int index) {
        float value = data[index];

        // move last value to cleared slot
        this.data[index] = data[--size];
        this.data[size] = 0f;

        return value;
    }

    public int removeValue(float value) {
        int index = indexOf(value);
        if (index > -1) {
            removeIndex(index);
            return index;
        }

        return -1;
    }

    public float removeLast() {
        float value = data[--size];
        this.data[size] = 0f;

        return value;
    }

    public void clear() {
        Arrays.fill(this.data, 0, this.size, 0f);
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
            result = 31 * result + Float.hashCode(this.data[i]);
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FloatBag)) {
            return false;
        }

        FloatBag other = (FloatBag) obj;
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
        StringBuilder builder = new StringBuilder().append("FloatBag(");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(data[i]);
        }
        return builder.append(")").toString();
    }

}
