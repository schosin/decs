package foo;

import de.schosin.decs.api.annotations.components.Component;

@Component
public interface Position {

    int x();

    void x(int value);

    int y();

    void y(int value);

}