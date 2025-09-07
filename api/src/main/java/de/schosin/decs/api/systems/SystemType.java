package de.schosin.decs.api.systems;

import java.util.concurrent.Callable;

import de.schosin.decs.api.entities.EntityArchetype;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * <p>
 * Marker interface for generated system code.
 * Systems are classes that contain methods annotated with {@link de.schosin.decs.api.annotations.system system annotations}.
 * Systems may also contain abstract methods annotated with {@link de.schosin.decs.api.annotations.utils utility annotations}.
 */
public interface SystemType extends Callable<Void> {

    void offerArchetype(EntityArchetype archetype);

    void runSystem();

    @Override
    default Void call() throws Exception {
        runSystem();
        return null;
    }

}
