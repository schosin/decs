package de.schosin.decs.core.systems;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.api.utils.collections.CollectionUtils;

public final class ParallelSystemData implements SystemData {

    private final List<SystemData> systems;
    private final ExecutorService executor;

    public ParallelSystemData(List<SystemData> systems, ExecutorService executor) {
        this.systems = CollectionUtils.listOf(systems);
        this.executor = executor;
    }

    @Override
    public SystemType getInstance(InternalWorld world) {
        List<SystemType> systems = this.systems.stream()
                .map(data -> data.getInstance(world))
                .collect(Collectors.toList());

        return new ParallelSystemType(executor, systems);
    }

    public List<SystemData> systems() {
        return this.systems;
    }

    public ExecutorService executor() {
        return this.executor;
    }

}
