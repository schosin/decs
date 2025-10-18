package foo;

import de.schosin.decs.api.annotations.components.Component;

@Component
public interface Velocity {

    float vx();

    void vx(float value);

    float vy();

    void vy(float value);

}
