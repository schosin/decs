package de.schosin.decs.it.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import de.schosin.decs.api.World;

class Simulation {

    private final World world;

    private final long durationMs;
    private final long sleep;

    private volatile boolean running;
    private volatile Throwable exception;

    Simulation(World world, Duration duration, long sleep) {
        this.world = world;
        this.durationMs = duration.toMillis();
        this.sleep = sleep;
    }

    public final void run() {
        assertThat(running).as("Simulation not already started").isFalse();

        this.running = true;
        new Thread(this::runSimulation, "simulation-thread").start();
    }

    private void runSimulation() {
        long start = System.currentTimeMillis();

        while (this.running) {
            try {
                world.process();

                Thread.sleep(sleep);

                if (System.currentTimeMillis() - start >= durationMs) {
                    this.running = false;
                }
            } catch (Throwable ex) {
                this.running = false;
                this.exception = ex;
            }
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isStopped() {
        return !this.running;
    }

    public Throwable getException() {
        return this.exception;
    }

}
