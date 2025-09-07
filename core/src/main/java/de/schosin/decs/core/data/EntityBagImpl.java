package de.schosin.decs.core.data;

import java.util.Arrays;
import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.decs.api.entities.EntityRef;
import de.schosin.decs.api.utils.collections.EntityBag;
import de.schosin.decs.api.utils.pool.Pooled;
import de.schosin.decs.core.CoreWorld;

public final class EntityBagImpl implements EntityBag, Pooled {

    private final CoreWorld world;

    private EntityRef[] data;
    private int size;

    public EntityBagImpl(CoreWorld world) {
        this.world = world;

        this.data = new EntityRef[1024];
        this.size = 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public void add(@NonNull EntityRef entity) {
        ensureCapacity(size);
        this.data[size++] = entity;
    }

    @Override
    public void addAll(EntityBag other) {
        if (other.isEmpty()) {
            return;
        }

        EntityRef[] data = ((EntityBagImpl) other).data;
        ensureCapacity(size - 1 + data.length);

        for (int i = 0, s = other.size(); i < s; i++) {
            this.data[this.size++] = data[i];
        }
    }

    @Override
    public boolean contains(EntityRef entity) {
        for (int i = 0; i < size; i++) {
            if (this.data[i].equals(entity)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public EntityRef get(int index) {
        if (index >= size) {
            return null;
        }

        return this.data[index];
    }

    @Override
    public EntityRef getUnsafe(int index) {
        return this.data[index];
    }

    @Override
    public void remove(int index) {
        if (index >= size) {
            return;
        }

        this.data[index] = this.data[--size];
        this.data[size] = null;
    }

    @Override
    public void removeUnsafe(int index) {
        this.data[index] = this.data[--size];
        this.data[size] = null;
    }

    @Override
    public void removeAll(EntityRef entity) {
        int index = indexOf(entity);
        while (index > -1) {
            removeUnsafe(index);
            index = indexOf(entity);
        }
    }

    @Override
    public void removeAll(EntityBag other) {
        EntityRef[] data = ((EntityBagImpl) other).data;
        for (int i = 0, s = other.size(); i < s; i++) {
            removeAll(data[i]);
        }
    }

    private int indexOf(EntityRef entity) {
        for (int i = 0; i < size; i++) {
            if (this.data[i].equals(entity)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void clear() {
        Arrays.fill(this.data, null);
        this.size = -1;
    }

    @Override
    public void free() {
        this.world.freeEntityBag(this);
    }

    @Override
    public Iterator<EntityRef> iterator() {
        throw new UnsupportedOperationException("iterator() not implemented yet");
    }

    @Override
    public void reset() {
        clear();

        // TODO reset more state
    }

    private void ensureCapacity(int index) {
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

}
