package de.schosin.decs.api.internal;

import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityRef;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * Implementation of {@link Entity} for usage of generated code.
 * User code should never use this type.
 */
public final class InternalEntity implements Entity {

    public final EntityArchetype archetype;

    public int index = -1;
    public int entityId = -1;

    public InternalEntity(EntityArchetype archetype) {
        this.archetype = archetype;
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
    public EntityRef createRef() {
        return archetype.createEntityRef(index);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Entity(id = ").append(entityId)
                .append(", components = ").append(archetype.getComponents())
                .append(")")
                .toString();
    }

}
