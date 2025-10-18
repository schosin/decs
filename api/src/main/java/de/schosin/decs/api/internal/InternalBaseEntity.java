package de.schosin.decs.api.internal;

import de.schosin.decs.api.entities.BaseEntity;
import de.schosin.decs.api.entities.EntityArchetype;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Extension of {@link BaseEntity} for usage of generated code.
 * User code should never use this type.
 */
public interface InternalBaseEntity extends BaseEntity {
    
    int id();

    EntityArchetype archetype();

    int index();

}
