package de.schosin.decs.examples.roguelike.components;

import de.schosin.decs.api.annotations.components.Component;
import de.schosin.decs.examples.roguelike.utils.Level;

@Component
public enum Pickup {

    COIN(Level.COIN);

    public final char glyph;

    private Pickup(char glyph) {
        this.glyph = glyph;
    }

}
