package de.schosin.decs.benchmark.components.decs;

import de.schosin.decs.api.annotations.components.Component;

@Component
public interface PositionInterface {

    float x();

    void x(float value);

    float y();

    void y(float value);

    default void add(float vx, float vy) {
        x(x() + vx);
        y(y() + vx);
    }

}
