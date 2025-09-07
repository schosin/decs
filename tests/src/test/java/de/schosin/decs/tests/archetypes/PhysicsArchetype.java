package de.schosin.decs.tests.archetypes;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;

public abstract class PhysicsArchetype {

    @Archetype
    public abstract void create(int count);

    @EntityInitializer
    final void create(int index, Position position, Velocity velocity) {
    }

}
