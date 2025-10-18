package foo;

import de.schosin.decs.api.annotations.components.Component;

@Component
public interface Position {

    float x();

    void x(float value);

    float y();

    void y(float value);

    default void add(float vx, float vy) {
        x(x() + vx);
        y(y() + vx);
    }

}
