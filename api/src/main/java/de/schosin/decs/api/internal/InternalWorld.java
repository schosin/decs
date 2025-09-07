package de.schosin.decs.api.internal;

import java.util.function.Function;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.builder.WorldBuilder;
import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.api.exceptions.InvalidSystemException;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * Extension of {@link World} for usage of generated code.
 * User code should never use this type.
 */
public interface InternalWorld extends World {

    /**
     * Gets an instance of a system registered with {@link WorldBuilder#add(Class)}.
     * 
     * @param <T> type of system
     * @param clazz class of system
     * @return instance of the system
     * @throws InvalidSystemException when system not found or not instantiable
     */
    <T> T getSystem(Class<T> clazz);

    /**
     * Retrieves the archetype given the component types.
     * 
     * @param components component types
     * @return archetype instance
     */
    EntityArchetype getEntityArchetype(Class<?>... components);

    /**
     * Flushes created, modified and deleted entities. <br>
     * This will cause {@link Inserted @Inserted} and {@link Removed @Remmoved} callbacks to be run for those entities.
     */
    void flushChanges();

    /**
     * Adds a {@link EntityArchetypeListener} for whenever an entity matches the given {@link Composition}.
     * 
     * <p>
     * Must return a new instance every time the factory function is called.
     * 
     * @param composition composition for the listener
     * @param factory factory of listener
     */
    void inserted(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory);

    /**
     * Adds a {@link EntityArchetypeListener} for whenever an entity no longer matches the given {@link Composition}.
     * 
     * <p>
     * Must return a new instance every time the factory function is called.
     * 
     * @param composition composition for the listener
     * @param factory factory of listener
     */
    void removed(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory);

}
