package foo;

import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.entities.EntityArchetype;

public class OfferArchetypeMethodSystem {

    @All
    @Inserted
    final void processEntity(int entityId) {
    }

    private final void offerArchetype(EntityArchetype archetype) {
    }

}
