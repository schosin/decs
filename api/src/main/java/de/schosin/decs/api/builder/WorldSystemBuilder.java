package de.schosin.decs.api.builder;

import de.schosin.decs.api.World;

import java.lang.reflect.InvocationTargetException;

public interface WorldSystemBuilder extends WorldBuilder {

    static WorldSystemBuilder create() {
        try {
            return (WorldSystemBuilder) Class.forName("de.schosin.decs.core.builder.CoreWorldSystemBuilder").getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() != null && ex.getTargetException().getClass().getName().startsWith("de.schosin")) {
                throw (RuntimeException) ex.getTargetException();
            }

            throw new IllegalArgumentException(String.format("Exception creating WorldSystemBuilder instance: %s", ex.getMessage()), ex);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException ex) {
            throw new IllegalArgumentException(String.format("Exception creating WorldSystemBuilder instance: %s", ex.getMessage()), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorldSystemBuilder singleton(Object singleton);

    /**
     * Adds the systems as a new unnamed system group.
     * Unnamed systems will be appended to the previous group if that one is also unnamed.
     *
     * @param systems systems to add
     * @return this builder
     */
    WorldSystemBuilder add(Class<?>... systems);

    /**
     * Adds the systems as a new named system group.
     *
     * <p>
     * A named system group can be {@link World#enableSystemGroup(String) enabled} or {@link World#disableSystemGroup(String) disabled} at runtime.
     *
     * @param groupName name of system group, must be unique
     * @param systems systems to add
     * @return this builder
     */
    WorldSystemBuilder add(String groupName, Class<?>... systems);

    /**
     * Adds the systems as a new disabled named system group.
     *
     * <p>
     * A named system group can be {@link World#enableSystemGroup(String) enabled} or {@link World#disableSystemGroup(String) disabled} at runtime.
     *
     * @param groupName name of system group, must be unique
     * @param systems systems to add
     * @return this builder
     */
    WorldSystemBuilder addDisabled(String groupName, Class<?>... systems);

}
