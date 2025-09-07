package de.schosin.decs.values;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.utils.pool.Pool;
import de.schosin.decs.tests.components.Acceleration;
import de.schosin.decs.tests.components.Faction;
import de.schosin.decs.tests.components.Player;
import de.schosin.decs.tests.components.Position;
import de.schosin.decs.tests.components.Velocity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class Components {
    private static final Map<Class<?>, ComponentMetadata<?>> LOOKUP;

    static {
        HashMap<Class<?>, ComponentMetadata<?>> result = new HashMap<Class<?>, ComponentMetadata<?>>();
        result.put(Position.class, new ComponentMetadata<>(0, Position.class, Pool.unbounded(1024, Position.class, Position::new)));
        result.put(Velocity.class, new ComponentMetadata<>(1, Velocity.class, Pool.unbounded(1024, Velocity.class, Velocity::new)));
        result.put(Faction.class, new ComponentMetadata<>(2, Faction.class, null));
        result.put(Acceleration.class, new ComponentMetadata<>(3, Acceleration.class, Pool.unbounded(1024, Acceleration.class, Acceleration::new)));
        result.put(Player.class, new ComponentMetadata<>(4, Player.class, null));

        LOOKUP = Collections.unmodifiableMap(result);
    }

    public static <T> ComponentMetadata<T> getMetadata(Class<T> type) {
        ComponentMetadata<T> result = (ComponentMetadata<T>) LOOKUP.get(type);
        if (result != null) {
            return result;
        }

        throw new IllegalArgumentException(String.format("Unknown component type: %s", type.getName()));
    }
}
