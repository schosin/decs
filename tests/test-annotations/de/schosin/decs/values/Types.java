package de.schosin.decs.values;

import de.schosin.decs.api.annotations.Generated;
import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.core.values.OtherValuesSystemImpl;
import de.schosin.decs.core.values.ValuesSystemImpl;
import de.schosin.decs.core.values.ValuesTest2;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Generated(
        value = "de.schosin.decs.codegen.DecsAnnotationProcessor",
        date = "2025-09-28T09:57:51.275802Z"
)
public final class Types {
    private static final Map<Class<?>, SystemMetadata> SYSTEMS;

    static {
        HashMap<Class<?>, SystemMetadata> systems = new HashMap<Class<?>, SystemMetadata>();
        systems.put(ValuesTest2.OtherValuesSystem.class, new SystemMetadata(ValuesTest2.OtherValuesSystem.class, OtherValuesSystemImpl.class, world -> new OtherValuesSystemImpl((InternalWorld) world), set()));
        systems.put(ValuesTest2.ValuesSystem.class, new SystemMetadata(ValuesTest2.ValuesSystem.class, ValuesSystemImpl.class, world -> new ValuesSystemImpl((InternalWorld) world), set()));

        SYSTEMS = Collections.unmodifiableMap(systems);
    }

    public static SystemMetadata getSystemMetadata(Class<?> clazz) {
        return SYSTEMS.get(clazz);
    }

    public static Map<Class<?>, Object> getUtilities(Object arg) {
        InternalWorld world = (InternalWorld) arg;

        HashMap<Class<?>, Object> result = new HashMap<Class<?>, Object>();

        return result;
    }

    private static <T> Set<T> set(T... values) {
        HashSet<T> result = new HashSet<T>();
        for (T value : values) {
            result.add(value);
        }

        return result;
    }
}
