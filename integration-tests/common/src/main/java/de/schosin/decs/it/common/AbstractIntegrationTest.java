package de.schosin.decs.it.common;

import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Random;

import org.assertj.core.api.Assertions;

import de.schosin.decs.api.World;

public abstract class AbstractIntegrationTest {

    protected static final Random RNG = new Random(0);

    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(5);
    private static final Duration MARGIN = Duration.ofSeconds(1);

    private static final long DEFAULT_SLEEP = 100L;

    protected Simulation runSimulation(World world) {
        return runSimulation(world, getSimulationDuration(), getSimultationSleep());
    }

    protected Simulation runSimulation(World world, Duration duration) {
        Assertions.setMaxStackTraceElementsDisplayed(100);

        return runSimulation(world, duration, getSimultationSleep());
    }

    protected Simulation runSimulation(World world, Duration duration, long sleep) {
        Simulation simulation = new Simulation(world, duration, sleep);
        simulation.run();

        await().atMost(duration.plus(MARGIN)).until(simulation::isStopped);

        Throwable exception = simulation.getException();
        if (exception != null) {
            fail("Simulation failed with an exception", exception);
        }

        return simulation;
    }

    protected Duration getSimulationDuration() {
        return DEFAULT_DURATION;
    }

    protected long getSimultationSleep() {
        return DEFAULT_SLEEP;
    }

}
