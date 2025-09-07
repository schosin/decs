package de.schosin.decs.api.utils.collections;

import java.util.Arrays;

/**
 * Map-like collection with an int key and an int value.
 * Unbound values will return {@code -1}.
 */
public class IntIntMap {

    private int[] data;

    public IntIntMap() {
        this(64);
    }

    public IntIntMap(int arraySize) {
        if (arraySize < 1) {
            throw new IllegalArgumentException("arraySize must be atleast 1");
        }

        this.data = new int[arraySize];
        Arrays.fill(this.data, -1);
    }

    public void set(int index, int value) {
        ensureCapacity(index);
        this.data[index] = value;
    }

    public void setUnsafe(int index, int value) {
        this.data[index] = value;
    }

    public int get(int index) {
        if (index < data.length) {
            return this.data[index];
        }

        return -1;
    }

    public int getUnsafe(int index) {
        return this.data[index];
    }

    public void remove(int index) {
        if (index < data.length) {
            this.data[index] = -1;
        }
    }

    public void clear() {
        Arrays.fill(this.data, -1);
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
                int oldLength = this.data.length;
                if (length > oldLength) {
                    this.data = Arrays.copyOf(data, length);
                    Arrays.fill(this.data, oldLength, length, -1);
                }
            }
        }
    }

}
