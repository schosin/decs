package de.schosin.decs.api.systems;

import de.schosin.decs.api.World;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 *
 * <p>
 * System invocation strategy that is used when {@link World#process()} is called.
 * When creating a World using {@link World#builder()}, a default implementation is used.
 * When creating a World using {@link World#builder(Class)}, a generated implementation of the passed interface is used.
 */
public interface SystemInvocation {

    /**
     * Runs the systems.
     */
    void runSystems();

    /**
     * Enables a named system group.
     */
    void enableSystemGroup(String name);

    /**
     * Disables a named system group.
     */
    void disableSystemGroup(String name);

    /**
     * Returns the systems.
     */
    SystemType[] getSystems();

}
