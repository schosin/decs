package de.schosin.decs.examples.roguelike.components;

import de.schosin.decs.api.annotations.components.Component;
import de.schosin.decs.api.utils.pool.Pooled;

@Component
public class Player implements Pooled {

    public int score;

    @Override
    public void reset() {
        this.score = 0;
    }

}
