package de.schosin.decs.core.builder.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import de.schosin.decs.api.builder.system.SystemBuilder;
import de.schosin.decs.api.exceptions.DuplicateSystemException;
import de.schosin.decs.api.exceptions.InvalidSystemException;
import de.schosin.decs.api.exceptions.MissingSystemDependencyException;
import de.schosin.decs.core.builder.CoreBuilder;
import de.schosin.decs.core.systems.ParallelSystemData;
import de.schosin.decs.core.systems.SystemData;
import de.schosin.decs.core.systems.SystemDataImpl;
import de.schosin.decs.values.SystemMetadata;
import de.schosin.decs.values.Types;

public final class CoreSystemBuilder implements SystemBuilder, CoreBuilder {

    private final CoreBuilder parent;

    private final List<SystemData> systems = new ArrayList<>();

    private ExecutorService executor;

    public CoreSystemBuilder(CoreBuilder parent, ExecutorService executor) {
        this.parent = parent;
        this.executor = executor;
    }

    @Override
    public boolean isSystemRegistered(Class<?> system) {
        return this.parent.isSystemRegistered(system) || this.systems.stream().anyMatch(data -> CoreBuilder.matches(system, data));
    }

    @Override
    public boolean isSingletonRegistered(Class<?> singleton) {
        return this.parent.isSingletonRegistered(singleton);
    }

    @Override
    public SystemBuilder executor(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        return this;
    }

    @Override
    public SystemBuilder with(Consumer<SystemBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    @Override
    public SystemBuilder parallel(Consumer<SystemBuilder> consumer) {
        return parallel(executor, consumer);
    }

    @Override
    public SystemBuilder parallel(ExecutorService executor, Consumer<SystemBuilder> consumer) {
        CoreSystemBuilder builder = new CoreSystemBuilder(this, executor);
        consumer.accept(builder);

        this.systems.add(builder.build());
        return this;
    }

    @Override
    public SystemBuilder add(Class<?>... systems) {
        for (Class<?> system : systems) {
            if (isSystemRegistered(system)) {
                throw new DuplicateSystemException(String.format("System '%s' has already been added.", system.getName()), system);
            }

            // Get system metadata
            SystemMetadata metadata = Types.getSystemMetadata(system);
            if (metadata == null) {
                throw new InvalidSystemException(
                        String.format("System metadata for class '%s' is not available. Make sure the annotation processor \"decs-codegen\" is enabled.", system.getName()),
                        system);
            }

            // Validate dependencies
            for (Class<?> dependency : metadata.dependencies()) {
                if (!isSingletonRegistered(dependency)) {
                    throw new MissingSystemDependencyException(
                            String.format("Dependency '%s' for system '%s' has not been added yet. Make sure dependencies are added before systems that use them.", dependency.getName(),
                                    system.getName()),
                            system);
                }
            }

            // Add system
            this.systems.add(new SystemDataImpl(metadata));
        }

        return this;
    }

    public ParallelSystemData build() {
        return new ParallelSystemData(systems, executor);
    }

}
