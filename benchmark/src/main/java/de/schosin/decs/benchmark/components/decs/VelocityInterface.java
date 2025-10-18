package de.schosin.decs.benchmark.components.decs;

import de.schosin.decs.api.annotations.components.Component;

@Component
public interface VelocityInterface {

    float vx();

    void vx(float value);

    float vy();

    void vy(float value);

}
