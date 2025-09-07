package de.schosin.decs.core.data;

import java.util.function.Function;

import de.schosin.decs.api.entities.Composition;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.api.entities.EntityArchetypeListener;

public final class Callback {

    private final Composition composition;
    private final Function<EntityArchetype, EntityArchetypeListener> factory;

    public Callback(Composition composition, Function<EntityArchetype, EntityArchetypeListener> factory) {
        this.composition = composition;
        this.factory = factory;
    }

    public Composition composition() {
        return composition;
    }

    public Function<EntityArchetype, EntityArchetypeListener> factory() {
        return factory;
    }

}