package de.schosin.decs.tests.components;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(ax, ay);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Acceleration other = (Acceleration) obj;
        return Float.floatToIntBits(this.ax) == Float.floatToIntBits(other.ax) && Float.floatToIntBits(this.ay) == Float.floatToIntBits(other.ay);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Acceleration [ax=").append(this.ax).append(", ay=").append(this.ay).append("]");
        return builder.toString();
    }

}
