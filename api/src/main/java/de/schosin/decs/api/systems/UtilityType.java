package de.schosin.decs.api.systems;

import de.schosin.decs.api.entities.EntityArchetype;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Marker interface for generated utility code.
 * Utilities are classes that contain methods annotated with {@link de.schosin.decs.api.annotations.utils utility annotations} and no {@link de.schosin.decs.api.annotations.system system annotations}.
 * 
 * <p>
 * Utilities are helper methods that can be used by systems by injecting them into methods.
 */
public interface UtilityType {

    void offerArchetype(EntityArchetype archetype);

}
