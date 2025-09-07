package de.schosin.decs.api.entities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.schosin.decs.api.utils.collections.CollectionUtils;

public interface Composition {

    /**
     * Returns a composition that matches all entities.
     * 
     * @return composition matching all entities
     */
    static Builder all() {
        return builder();
    }

    /**
     * Returns a composition builder that matches all entities that have 
     * all of the passed components.
     * 
     * @param components components that entities must have all of
     * @return composition builder
     */
    static Builder all(Class<?>... components) {
        return builder().all(components);
    }

    /**
     * Returns a composition builder that matches all entities that have 
     * atleast one of the passed components.
     *
     * @param components components that entities must have atleast one of
     * @return composition builder
     */
    static Builder one(Class<?>... components) {
        return builder().one(components);
    }

    /**
     * Returns a composition builder that matches all entities that have 
     * none one of the passed components.
     *
     * @param components components that entities must not have any of
     * @return composition builder
     */
    static Builder none(Class<?>... components) {
        return builder().none(components);
    }

    static Builder builder() {
        return new CompositionBuilderImpl();
    }

    interface Builder {

        /**
         * Adds the passed components to the components entities must have all of.
         * 
         * <p>
         * Components will be appended to existing list.
         * 
         * @param components components that entities must have all of
         * @return composition builder
         */
        Builder all(Class<?>... components);

        /**
         * Adds a new set of components entities must have atleast one of.
         * 
         * <p>
         * Unlike {@link #all(Class...)} and {@link #none(Class...)} this will create a
         * new group that is checked separately from previous calls to {@link #one(Class...)}.
         * 
         * @param components components that entities must have atleast one of
         * @return composition builder
         */
        Builder one(Class<?>... components);

        /**
         * Adds the passed components to the components entities must have none of.
         * 
         * <p>
         * Components will be appended to existing list.
         * 
         * @param components components that entities must not have any of
         * @return composition builder
         */
        Builder none(Class<?>... components);

        /**
         * Used by implementations to retrive an 
         */
        Composition build();

    }

    /**
     * Checks whether the given archetype matches this composition.
     * 
     * @param archetype archetype to match
     * @return true if components match this composition
     */
    boolean matches(EntityArchetype archetype);

}

final class CompositionImpl implements Composition {

    private final Set<Class<?>> all;
    private final Set<Set<Class<?>>> ones;
    private final Set<Class<?>> none;

    public CompositionImpl(Set<Class<?>> all, Set<Set<Class<?>>> ones, Set<Class<?>> none) {
        this.all = all;
        this.ones = ones;
        this.none = none;
    }

    @Override
    public boolean matches(EntityArchetype archetype) {
        List<Class<?>> components = archetype.getComponents();

        return (all.isEmpty() || all.stream().allMatch(components::contains))
                && (ones.isEmpty() || ones.stream().allMatch(one -> one.isEmpty() || one.stream().anyMatch(components::contains)))
                && (none.isEmpty() || none.stream().noneMatch(components::contains));
    }

}

final class CompositionBuilderImpl implements Composition.Builder {

    private final Set<Class<?>> all = new HashSet<>();
    private final Set<Set<Class<?>>> ones = new HashSet<>();
    private final Set<Class<?>> none = new HashSet<>();

    @Override
    public Composition.Builder all(Class<?>... components) {
        for (Class<?> component : components) {
            all.add(component);
        }

        return this;
    }

    @Override
    public Composition.Builder one(Class<?>... components) {
        ones.add(CollectionUtils.setOf(components));

        return this;
    }

    @Override
    public Composition.Builder none(Class<?>... components) {
        for (Class<?> component : components) {
            none.add(component);
        }

        return this;
    }

    @Override
    public Composition build() {
        return new CompositionImpl(
                CollectionUtils.setOf(all),
                CollectionUtils.setOf(ones.stream().map(CollectionUtils::setOf).collect(Collectors.toSet())),
                CollectionUtils.setOf(new HashSet<>(none)));
    }

}