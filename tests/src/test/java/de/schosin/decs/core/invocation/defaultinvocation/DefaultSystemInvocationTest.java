package de.schosin.decs.core.invocation.defaultinvocation;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.core.invocation.sequential.SequentialSystemInvocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSystemInvocationTest {

    @Test
    void testSystemInvocation() {
        var world = World.builder()
                .add("group1", DefaultA.class, DefaultB.class)
                .add("group2", DefaultC.class)
                .add(DefaultD.class)
                .build();

        var values = world.getValues();
        values.defaultSystemsList = new ArrayList<>();

        world.process();
        assertThat(values.defaultSystemsList).containsExactly("DefaultA", "DefaultB", "DefaultC", "DefaultD");
    }

    @Test
    void testSystemGroups() {
        var world = World.builder()
                .add("group1", DefaultA.class, DefaultB.class)
                .add("group2", DefaultC.class)
                .add(DefaultD.class)
                .build();

        var values = world.getValues();
        values.defaultSystemsList = new ArrayList<>();

        world.process();
        assertThat(values.defaultSystemsList).containsExactly("DefaultA", "DefaultB", "DefaultC", "DefaultD");

        values.defaultSystemsList.clear();
        world.disableSystemGroup("group1");
        world.process();
        assertThat(values.defaultSystemsList).containsExactly("DefaultC", "DefaultD");

        values.defaultSystemsList.clear();
        world.enableSystemGroup("group1");
        world.disableSystemGroup("group2");
        world.process();
        assertThat(values.defaultSystemsList).containsExactly("DefaultA", "DefaultB", "DefaultD");

        values.defaultSystemsList.clear();
        world.enableSystemGroup("group2");
        world.process();
        assertThat(values.defaultSystemsList).containsExactly("DefaultA", "DefaultB", "DefaultC", "DefaultD");
    }

    public static class DefaultA {
        @SystemProcessor
        final void process(@Value("defaultSystemsList") List<String> list) {
            list.add("DefaultA");
        }
    }

    public static class DefaultB {
        @SystemProcessor
        final void process(@Value("defaultSystemsList") List<String> list) {
            list.add("DefaultB");
        }
    }

    public static class DefaultC {
        @SystemProcessor
        final void process(@Value("defaultSystemsList") List<String> list) {
            list.add("DefaultC");
        }
    }

    public static class DefaultD {
        @SystemProcessor
        final void process(@Value("defaultSystemsList") List<String> list) {
            list.add("DefaultD");
        }
    }

}