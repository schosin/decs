package de.schosin.decs.core.systems;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.exceptions.ParallelExecutionException;
import de.schosin.decs.api.systems.SystemType;

public final class ParallelSystemType implements SystemType {

    private final ExecutorService executor;
    private final List<SystemType> systems;
    private final int size;

    public ParallelSystemType(ExecutorService executor, List<SystemType> systems) {
        this.executor = executor;
        this.systems = systems;
        this.size = systems.size();
    }

    public List<SystemType> getSystems() {
        return this.systems;
    }

    @Override
    public void offerArchetype(EntityArchetype archetype) {
        for (SystemType system : systems) {
            system.offerArchetype(archetype);
        }
    }

    @Override
    public void runSystem() {
        try {
            List<Future<Void>> futures = executor.invokeAll(systems);

            for (int i = 0, s = size; i < s; i++) {
                futures.get(i).get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new ParallelExecutionException(ex);
        }
    }

}
