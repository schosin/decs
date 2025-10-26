package de.schosin.decs.core.systems;

import de.schosin.decs.api.systems.SystemType;
import de.schosin.decs.core.CoreWorld;

import java.util.Arrays;
import java.util.stream.Stream;

public final class SystemGroup {

    private final CoreWorld world;

    private final String name;
    private final boolean unnamed;
    private final SystemType[] systems;

    private boolean enabled;

    public SystemGroup(CoreWorld world, String name, boolean unnamed, SystemType[] systems, boolean enabled) {
        this.world = world;

        this.name = name;
        this.unnamed = unnamed;
        this.systems = systems;

        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public boolean isUnnamed() {
        return unnamed;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void runSystems() {
        if (!enabled) {
            return;
        }

        for (int i = 0, s = systems.length; i < s; i++) {
            this.systems[i].runSystem();
            this.world.flushChanges();
        }
    }

    Stream<SystemType> getSystems() {
        return Arrays.stream(this.systems);
    }

    @Override
    public String toString() {
        return "SystemGroup{" +
                "name='" + name + '\'' +
                (unnamed ? "" : ", enabled=" + enabled) +
                ", systems=" + Arrays.toString(systems) +
                '}';
    }

}
