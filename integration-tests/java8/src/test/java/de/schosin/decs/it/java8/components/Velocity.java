package de.schosin.decs.it.java8.components;

import de.schosin.decs.api.annotations.components.Component;

@Component
public final class Velocity {

    public float vx;
    public float vy;

    public Velocity() {
    }

    public Velocity(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
    }

    public Velocity with(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;

        return this;
    }

}
