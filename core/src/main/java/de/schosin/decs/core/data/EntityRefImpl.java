package de.schosin.decs.core.data;

import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityRef;
import de.schosin.decs.api.internal.InternalBaseEntity;
import de.schosin.decs.api.utils.pool.Pooled;

public final class EntityRefImpl implements EntityRef, InternalBaseEntity, Pooled {

    EntityArchetypeImpl archetype;
    int index;
    int entityId;

    EntityRefImpl() {
    }

    EntityRefImpl set(EntityArchetypeImpl archetype, int index, int entityId) {
        this.archetype = archetype;
        this.index = index;
        this.entityId = entityId;

        return this;
    }

    EntityRefImpl set(int index, int entityId) {
        this.index = index;
        this.entityId = entityId;

        return this;
    }

    @Override
    public int id() {
        return entityId;
    }

    @Override
    public void delete() {
        this.archetype.deleteEntity(index);
    }

    @Override
    public <T> T getComponent(Class<T> clazz) {
        return (T) archetype.getData(clazz).get(index);
    }

    @Override
    public boolean valid() {
        return index != -1;
    }

    @Override
    public EntityRef copy() {
        if (!valid()) {
            throw new IllegalStateException("EntityRef not valid");
        }

        return archetype.createEntityRef(index);
    }

    @Override
    public void free() {
        if (this.archetype != null) {
            this.archetype.freeEntityRef(this);
        }
    }

    @Override
    public EntityArchetype archetype() {
        return archetype;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public void reset() {
        this.archetype = null;
        this.index = -1;
        this.entityId = -1;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityRefImpl [entityId=").append(this.entityId).append(", index=").append(this.index).append(", archetype=").append(this.archetype).append("]");
        return builder.toString();
    }

}
