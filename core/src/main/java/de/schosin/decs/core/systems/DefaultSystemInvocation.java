package de.schosin.decs.core.systems;

import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.api.systems.SystemType;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DefaultSystemInvocation implements SystemInvocation {

    private final SystemGroup[] groups;
    private final Map<String, SystemGroup> lookup;

    public DefaultSystemInvocation(SystemGroup[] groups) {
        this.groups = groups;
        this.lookup = Arrays.stream(groups).filter(group -> !group.isUnnamed()).collect(Collectors.toMap(SystemGroup::getName, Function.identity()));
    }

    @Override
    public void runSystems() {
        for (int i = 0, s = groups.length; i < s; i++) {
            this.groups[i].runSystems();
        }
    }

    @Override
    public void enableSystemGroup(String name) {
        // NPE is okay, user error does not justify a null check
        this.lookup.get(name).enable();
    }

    @Override
    public void disableSystemGroup(String name) {
        // NPE is okay, user error does not justify a null check
        this.lookup.get(name).disable();
    }

    @Override
    public SystemType[] getSystems() {
        return Arrays.stream(groups).flatMap(SystemGroup::getSystems).toArray(SystemType[]::new);
    }

}
