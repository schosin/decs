package de.schosin.decs.examples.roguelike.components;

import de.schosin.decs.api.annotations.components.Component;

@Component
public class Position {

    public int x;
    public int y;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Position [x=").append(this.x).append(", y=").append(this.y).append("]");
        return builder.toString();
    }

}
