package de.schosin.decs.core.builder;

import de.schosin.decs.api.builder.WorldSystemBuilder;
import de.schosin.decs.api.exceptions.InvalidSystemException;
import de.schosin.decs.api.exceptions.MissingSystemDependencyException;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.core.CoreWorld;
import de.schosin.decs.core.systems.DefaultSystemInvocation;
import de.schosin.decs.core.systems.SystemGroup;
import de.schosin.decs.values.SystemMetadata;
import de.schosin.decs.values.Types;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused") // used reflectively in api module
public final class CoreWorldSystemBuilder extends AbstractCoreWorldBuilder<CoreWorldSystemBuilder> implements WorldSystemBuilder {

    private final List<Class<?>> systems = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final Set<String> usedNames = new HashSet<>();

    @Override
    public WorldSystemBuilder add(Class<?>... systems) {
        for (Class<?> system : systems) {
            this.systems.add(system);
        }

        return this;
    }

    @Override
    public WorldSystemBuilder add(String groupName, Class<?>... systems) {
        finishUnnamedGroup();

        if (!this.usedNames.add(groupName)) {
            throw new IllegalArgumentException("SystemGroup name already used: " + groupName);
        }

        this.groups.add(new Group(groupName, false, Arrays.asList(systems), true));
        return this;
    }

    @Override
    public WorldSystemBuilder addDisabled(String groupName, Class<?>... systems) {
        finishUnnamedGroup();

        if (!this.usedNames.add(groupName)) {
            throw new IllegalArgumentException("SystemGroup name already used: " + groupName);
        }

        this.groups.add(new Group(groupName, false, Arrays.asList(systems), false));
        return this;
    }

    @Override
    protected Function<InternalWorld, SystemInvocation> getSystemInvocation() {
        finishUnnamedGroup();

        return arg -> {
            CoreWorld world = (CoreWorld) arg;

            SystemGroup[] systemGroups = this.groups.stream()
                    .map(group -> group.build(world))
                    .toArray(SystemGroup[]::new);

            return new DefaultSystemInvocation(systemGroups);
        };
    }

    private void finishUnnamedGroup() {
        if (systems.isEmpty()) {
            return;
        }

        String groupName = String.format("Unnamed group (index %d)", this.groups.size());
        if (!this.usedNames.add(groupName)) {
            throw new IllegalStateException("SystemGroup name already used: " + groupName);
        }

        this.groups.add(new Group(groupName, true, new ArrayList<>(this.systems), true));
        this.systems.clear();
    }

    private final class Group {

        private final String name;
        private final boolean unnamed;
        private final List<Class<?>> systems;
        private final boolean enabled;

        public Group(String name, boolean unnamed, List<Class<?>> systems, boolean enabled) {
            this.name = name;
            this.unnamed = unnamed;
            this.systems = systems;
            this.enabled = enabled;
        }

        public SystemGroup build(CoreWorld world) {
            SystemType[] systems = this.systems.stream()
                    .map(type -> (SystemType) Types.getSystemMetadata(type).constructor().apply(world))
                    .toArray(SystemType[]::new);

            return new SystemGroup(world, name, unnamed, systems, enabled);
        }

        private SystemType build(Class<?> system, InternalWorld world) {
            SystemMetadata metadata = Types.getSystemMetadata(system);
            if (metadata == null) {
                throw new InvalidSystemException(
                        String.format("System metadata for class '%s' is not available. Make sure the annotation processor \"decs-codegen\" is enabled.", system.getName()),
                        system);
            }

            // Validate singleton dependencies
            for (Class<?> dependency : metadata.dependencies()) {
                if (!isSingletonRegistered(dependency)) {
                    throw new MissingSystemDependencyException(
                            String.format("Dependency '%s' for system '%s' has not been added. Make sure dependencies are added before they are used.", dependency.getName(), system.getName()),
                            system);
                }
            }

            return (SystemType) metadata.constructor().apply(world);
        }

    }

}
