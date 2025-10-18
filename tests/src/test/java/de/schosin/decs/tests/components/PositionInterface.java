package de.schosin.decs.tests.components;

import de.schosin.decs.api.annotations.components.Component;

@Component
public interface PositionInterface {

    int x();

    void x(int value);

    int y();

    void y(int value);

}
