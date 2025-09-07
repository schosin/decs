package de.schosin.decs.it.java8.components;

import de.schosin.decs.api.annotations.components.Component;

@Component
public final class Acceleration {

    public float ax;
    public float ay;

    public Acceleration() {
    }

    public Acceleration(float ax, float ay) {
        this.ax = ax;
        this.ay = ay;
    }

    public Acceleration with(float ax, float ay) {
        this.ax = ax;
        this.ay = ay;

        return this;
    }

}
