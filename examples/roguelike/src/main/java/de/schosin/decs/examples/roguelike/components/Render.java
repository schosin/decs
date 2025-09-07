package de.schosin.decs.examples.roguelike.components;

import de.schosin.decs.api.annotations.components.Component;

@Component
public class Render {

    public char glyph;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Render [glyph=").append(this.glyph).append("]");
        return builder.toString();
    }

}
