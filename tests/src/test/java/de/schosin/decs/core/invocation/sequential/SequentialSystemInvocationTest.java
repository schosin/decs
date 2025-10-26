package de.schosin.decs.core.invocation.sequential;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SequentialSystemInvocationTest {

    @Test
    void testSystemInvocation() {
        var world = World.builder(SequentialSystemInvocation.class)
                .build();

        var values = world.getValues();
        values.sequentialSystemsList = new ArrayList<>();

        world.process();
        assertThat(values.sequentialSystemsList).containsExactly("SequentialA", "SequentialB", "SequentialC", "SequentialD");
    }

    @Test
    void testSystemGroups() {
        var world = World.builder(SequentialSystemInvocation.class)
                .build();

        var values = world.getValues();
        values.sequentialSystemsList = new ArrayList<>();

        world.process();
        assertThat(values.sequentialSystemsList).containsExactly("SequentialA", "SequentialB", "SequentialC", "SequentialD");

        values.sequentialSystemsList.clear();
        world.disableSystemGroup("group1");
        world.process();
        assertThat(values.sequentialSystemsList).containsExactly("SequentialC", "SequentialD");

        values.sequentialSystemsList.clear();
        world.enableSystemGroup("group1");
        world.disableSystemGroup("group2");
        world.process();
        assertThat(values.sequentialSystemsList).containsExactly("SequentialA", "SequentialB", "SequentialD");

        values.sequentialSystemsList.clear();
        world.enableSystemGroup("group2");
        world.process();
        assertThat(values.sequentialSystemsList).containsExactly("SequentialA", "SequentialB", "SequentialC", "SequentialD");
    }

    public static class SequentialA {
        @SystemProcessor
        final void process(@Value("sequentialSystemsList") List<String> list) {
            list.add("SequentialA");
        }
    }

    public static class SequentialB {
        @SystemProcessor
        final void process(@Value("sequentialSystemsList") List<String> list) {
            list.add("SequentialB");
        }
    }

    public static class SequentialC {
        @SystemProcessor
        final void process(@Value("sequentialSystemsList") List<String> list) {
            list.add("SequentialC");
        }
    }

    public static class SequentialD {
        @SystemProcessor
        final void process(@Value("sequentialSystemsList") List<String> list) {
            list.add("SequentialD");
        }
    }

}