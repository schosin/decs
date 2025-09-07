package de.schosin.decs.api.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.exceptions.InvalidComponentException;
import de.schosin.decs.api.utils.collections.CollectionUtils;
import de.schosin.decs.values.ComponentMetadata;
import de.schosin.decs.values.Components;

/**
 * <b>INTERNAL API:</b> This type is not intended to be used directly.
 * 
 * Represents the changes by a {@link Transmute @Transmute} method, containing the ids of the added and removed components.
 */
public final class Transmutation {

    public static Builder builder() {
        return new Builder();
    }

    private static final AtomicInteger ID = new AtomicInteger(0);

    private final int id;
    private final List<ComponentMetadata<?>> add;
    private final List<ComponentMetadata<?>> remove;

    private Transmutation(List<ComponentMetadata<?>> add, List<ComponentMetadata<?>> remove) {
        this.id = ID.getAndIncrement();
        this.add = CollectionUtils.listOf(add);
        this.remove = CollectionUtils.listOf(remove);
    }

    public int getId() {
        return this.id;
    }

    public List<ComponentMetadata<?>> getAdd() {
        return this.add;
    }

    public List<ComponentMetadata<?>> getRemove() {
        return this.remove;
    }

    @Override
    public int hashCode() {
        return Objects.hash(add, remove);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transmutation other = (Transmutation) obj;
        return Objects.equals(this.add, other.add) && Objects.equals(this.remove, other.remove);
    }

    public static final class Builder {

        private static final Map<Set<ComponentMetadata<?>>, Map<Set<ComponentMetadata<?>>, Transmutation>> TRANSMUTATIONS = new HashMap<>();

        private final Set<ComponentMetadata<?>> add = new HashSet<>();
        private final Set<ComponentMetadata<?>> remove = new HashSet<>();

        public Builder add(Class<?>... components) {
            for (Class<?> component : components) {
                ComponentMetadata<?> metadata = Components.getMetadata(component);
                if (metadata == null) {
                    throw new InvalidComponentException(String.format("Metadata for component type '%s' not available.", component.getName()), component);
                }

                this.add.add(metadata);
            }

            return this;
        }

        public Builder remove(Class<?>... components) {
            for (Class<?> component : components) {
                ComponentMetadata<?> metadata = Components.getMetadata(component);
                if (metadata == null) {
                    throw new InvalidComponentException(String.format("Metadata for component type '%s' not available.", component.getName()), component);
                }

                this.remove.add(metadata);
            }

            return this;
        }

        public Transmutation build() {
            if (!Collections.disjoint(add, remove)) {
                throw new IllegalArgumentException(String.format("Added and removed components have an overlap: %s // %s", add, remove));
            }

            return TRANSMUTATIONS
                    .computeIfAbsent(this.add, ignore -> new HashMap<>())
                    .computeIfAbsent(this.remove, ignore -> new Transmutation(
                            this.add.stream().sorted(ComponentMetadata.COMPARATOR).collect(Collectors.toList()),
                            this.remove.stream().sorted(ComponentMetadata.COMPARATOR).collect(Collectors.toList())));
        }

    }

}
