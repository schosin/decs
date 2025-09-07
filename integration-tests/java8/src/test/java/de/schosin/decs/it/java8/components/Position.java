package de.schosin.decs.it.java8.components;

import de.schosin.decs.api.annotations.components.Component;

@Component
public final class Position {

    public float x;
    public float y;

    public Position() {
    }

    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Position with(float x, float y) {
        this.x = x;
        this.y = y;

        return this;
    }

}
