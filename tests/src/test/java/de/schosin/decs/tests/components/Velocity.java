package de.schosin.decs.tests.components;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(vx, vy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Velocity other = (Velocity) obj;
        return Float.floatToIntBits(this.vx) == Float.floatToIntBits(other.vx) && Float.floatToIntBits(this.vy) == Float.floatToIntBits(other.vy);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Velocity [vx=").append(this.vx).append(", vy=").append(this.vy).append("]");
        return builder.toString();
    }

}
