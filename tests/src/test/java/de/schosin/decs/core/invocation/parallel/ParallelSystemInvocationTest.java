package de.schosin.decs.core.invocation.parallel;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class ParallelSystemInvocationTest {

    @Test
    void testSystemInvocation() {
        var world = World.builder(ParallelSystemInvocation.class)
                .build();

        var values = world.getValues();
        values.parallelSystemsList = new CopyOnWriteArrayList<>();

        world.process();
        assertThat(values.parallelSystemsList)
                .hasSize(5)
                .containsSubsequence("ParallelA", "ParallelB", "ParallelC", "ParallelE")
                .containsSubsequence("ParallelA", "ParallelB", "ParallelD", "ParallelE");
    }

    @Test
    void testSystemGroups() {
        var world = World.builder(ParallelSystemInvocation.class)
                .build();

        var values = world.getValues();
        values.parallelSystemsList = new CopyOnWriteArrayList<>();

        world.process();
        assertThat(values.parallelSystemsList)
                .hasSize(5)
                .containsSubsequence("ParallelA", "ParallelB", "ParallelC", "ParallelE")
                .containsSubsequence("ParallelA", "ParallelB", "ParallelD", "ParallelE");

        values.parallelSystemsList.clear();
        world.disableSystemGroup("group1");
        world.process();
        assertThat(values.parallelSystemsList)
                .hasSize(3)
                .containsSubsequence("ParallelC", "ParallelE")
                .containsSubsequence("ParallelD", "ParallelE");

        values.parallelSystemsList.clear();
        world.enableSystemGroup("group1");
        world.disableSystemGroup("group2");
        world.process();
        assertThat(values.parallelSystemsList).containsExactly("ParallelA", "ParallelB", "ParallelE");

        values.parallelSystemsList.clear();
        world.enableSystemGroup("group2");
        world.process();
        assertThat(values.parallelSystemsList)
                .hasSize(5)
                .containsSubsequence("ParallelA", "ParallelB", "ParallelC", "ParallelE")
                .containsSubsequence("ParallelA", "ParallelB", "ParallelD", "ParallelE");
    }

    public static class ParallelA {
        @SystemProcessor
        final void process(@Value("parallelSystemsList") List<String> list) {
            list.add("ParallelA");
        }
    }

    public static class ParallelB {
        @SystemProcessor
        final void process(@Value("parallelSystemsList") List<String> list) {
            list.add("ParallelB");
        }
    }

    public static class ParallelC {
        @SystemProcessor
        final void process(@Value("parallelSystemsList") List<String> list) {
            list.add("ParallelC");
        }
    }

    public static class ParallelD {
        @SystemProcessor
        final void process(@Value("parallelSystemsList") List<String> list) {
            list.add("ParallelD");
        }
    }

    public static class ParallelE {
        @SystemProcessor
        final void process(@Value("parallelSystemsList") List<String> list) {
            list.add("ParallelE");
        }
    }

}