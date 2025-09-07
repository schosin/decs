package de.schosin.decs.tests.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Position other = (Position) obj;
        return Float.floatToIntBits(this.x) == Float.floatToIntBits(other.x) && Float.floatToIntBits(this.y) == Float.floatToIntBits(other.y);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Position [x=").append(this.x).append(", y=").append(this.y).append("]");
        return builder.toString();
    }

}

// @Component
interface Pos {

    float x();

    void x(float x);

    float y();

    void y(float y);

}

// TODO generated type, allows "@PosComponent x" for fasted read-only access
@Target(ElementType.PARAMETER)
@interface PosComponent {}

final class PosImpl implements Pos {

    private final float[] x = null; // TODO from generated EntityArchetypeImpl
    private final float[] y = null;

    private int index;

    @Override
    public float x() {
        return x[index];
    }

    @Override
    public void x(float x) {
        this.x[index] = x;
    }

    @Override
    public float y() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void y(float y) {
        // TODO Auto-generated method stub

    }

}
