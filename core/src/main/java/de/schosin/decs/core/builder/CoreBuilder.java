package de.schosin.decs.core.builder;

import de.schosin.decs.core.systems.ParallelSystemData;
import de.schosin.decs.core.systems.SystemData;
import de.schosin.decs.core.systems.SystemDataImpl;

public interface CoreBuilder {

    /**
     * Checks whether a system is already registered.
     * 
     * @param system system to check
     * @return true if already registered
     */
    boolean isSystemRegistered(Class<?> system);
    
    /**
     * Checks whether a singleotn is already registed.
     * 
     * @param singleton singleton to check
     * @return true if already registered
     */
    boolean isSingletonRegistered(Class<?> singleton);

    static boolean matches(Class<?> system, SystemData existing) {
        if (existing instanceof SystemDataImpl) {
            SystemDataImpl data = (SystemDataImpl) existing;
            return data.metadata().clazz().equals(system);
        }

        ParallelSystemData parallel = (ParallelSystemData) existing;
        return parallel.systems().stream().anyMatch(sys -> matches(system, sys));
    }

}
