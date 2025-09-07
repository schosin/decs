package de.schosin.decs.examples.roguelike.systems;

import java.util.random.RandomGenerator;

import de.schosin.decs.api.annotations.Singleton;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.examples.roguelike.components.Cat;
import de.schosin.decs.examples.roguelike.components.Position;
import de.schosin.decs.examples.roguelike.singletons.Screen;
import de.schosin.decs.examples.roguelike.utils.Level;

/**
 * System for moving cats around and picking up coins.
 */
public class CatSystem {

    private static final RandomGenerator RNG = RandomGenerator.getDefault();

    /**
     * Adds a reference to the cat entity to the level when inserted. <br />
     * Used in the process method and by the {@link InputSystem}.
     */
    @All({ Cat.class, Position.class })
    @Inserted
    final void setPlayerPos(Entity entity, Position pos, @Value("level") Level level) {
        level.set(pos.x, pos.y, Level.CAT, entity.createRef());
    }

    /**
     * Randomly chooses a direction for the cat to walk to.
     * Will be called for all entities that have a {@link Cat} and a {@link Position} component.
     * 
     * @param cat cat component for the score
     * @param pos position component of the entity
     * @param level current level to check where the cat moves to
     * @param screen terminal for updating the cat glyph
     */
    @All({ Cat.class, Position.class })
    @EntityProcessor
    final void process(Cat cat, Position pos, @Value("level") Level level, @Value("playerPos") Position playerPos, @Singleton Screen screen) {
        var x = pos.x;
        var y = pos.y;

        switch (RNG.nextInt(4)) {
            case 0 -> y--;
            case 1 -> x--;
            case 2 -> y++;
            case 3 -> x++;
        }

        if (x == playerPos.x && y == playerPos.y) {
            return;
        }

        var c = level.get(x, y);
        switch (c) {
            case Level.SPACE -> {
                // Update level
                var ref = level.set(pos.x, pos.y, Level.SPACE, null);
                level.set(x, y, Level.CAT, ref);

                // Update position component
                screen.drawChar(pos.x, pos.y, ' '); // clear old position

                pos.x = x;
                pos.y = y;
            }
            case Level.WALL -> {
                // movement blocked
            }
            case Level.COIN -> {
                // Update level
                var ref = level.set(pos.x, pos.y, Level.SPACE, null);

                // Update position component
                screen.drawChar(pos.x, pos.y, ' '); // clear old position

                pos.x = x;
                pos.y = y;

                // Remove coin entity and increment current score
                level.set(x, y, Level.CAT, ref);
                cat.score++;
            }
            case Level.CAT -> {
                // Update level
                var ref = level.set(pos.x, pos.y, Level.SPACE, null);

                // Update position component
                screen.drawChar(pos.x, pos.y, ' '); // clear old position

                pos.x = x;
                pos.y = y;

                // Remove cat entity and increment current score by that cats score
                var otherCat = level.set(x, y, Level.CAT, ref);
                cat.score += otherCat.getComponent(Cat.class).score;

                otherCat.delete();
            }
            default -> throw new IllegalStateException("Unknown level character at (%d, %d): %s".formatted(x, y, c));
        }
    }

}
