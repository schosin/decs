package de.schosin.decs.examples.roguelike.systems;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Singleton;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Count;
import de.schosin.decs.examples.roguelike.components.Cat;
import de.schosin.decs.examples.roguelike.components.Moving;
import de.schosin.decs.examples.roguelike.components.Player;
import de.schosin.decs.examples.roguelike.components.Position;
import de.schosin.decs.examples.roguelike.components.Render;
import de.schosin.decs.examples.roguelike.singletons.Screen;
import de.schosin.decs.examples.roguelike.utils.Level;

/**
 * System for rendering the player, level and the cats, as well as the scores.
 */
@All({ Position.class, Render.class })
public abstract class RenderSystem {

    /**
     * Resets the combined score of all cats value to zero.
     */
    @SystemProcessor
    final void resetCatScore(World world) {
        world.getValues().catScore = 0;
    }

    /**
     * Calculates the combined score of all cats.
     */
    @EntityProcessor
    @All(Cat.class)
    final void calcCatScore(Cat cat, World world) {
        // this could also be calculated when modifying Cat#score instead, which would be more performant.
        world.getValues().catScore += cat.score;
    }

    /**
     * Renders an info line below the level with the current score and total number of entities.
     * 
     * @param player player component for accessing the score
     * @param screen for writing to the terminal
     */
    @EntityProcessor
    @All(Player.class)
    final void renderInfo(Player player, @Value("catScore") int catScore, @Singleton Screen screen) {
        var text = "Score: %3d | Cat score: %3d | Entities: %d".formatted(player.score, catScore, getCount());
        screen.drawText(2, screen.getHeight() - 1, text);
    }

    /**
     * {@link Count @Count} is a utility method that returns the number of entities that match the {@link de.schosin.decs.api.annotations.composition composition annotations}.
     * 
     * <p>
     * Using {@code @All} without parameters will select all entities, regardless of their components.
     * Using {@code @All} with parameters will select all entities that have all of those components.
     * Using {@code @One} with parameters will select all entities that have atleast one of those components. {@code One} can be repeated for multiple conditions.
     * Using {@code @None} with parameters will select all entities that have none of those components.
     */
    @Count
    @All
    abstract int getCount();

    /**
     * Renders moving entities, in this case only the player entity.
     * 
     * @param pos position component
     * @param render render component
     * @param screen terminal to write the characters glyph at its position
     */
    @EntityProcessor
    @All({ Position.class, Render.class, Moving.class })
    final void renderMoving(Position pos, Render render, @Singleton Screen screen) {
        screen.drawChar(pos.x, pos.y, render.glyph);
    }

    /**
     * Writes static entities to the buffer whenever they are created, in this case the walls and coins.
     * 
     * @param pos position component
     * @param render render component
     * @param screen terminal to write the characters glyph at its position
     */
    @Inserted
    @None(Moving.class)
    @All({ Position.class, Render.class })
    final void renderNonMoving(Position pos, Render render, @Singleton Screen screen) {
        screen.drawChar(pos.x, pos.y, render.glyph);
    }

    /**
     * Removes the static entities from the buffer when they are removed.
     * 
     * <p>
     * This is called whenever the player or cat picks up a coin entity by moving onto it. <br />
     * This is handled in {@link InputSystem#process(String, Position, Player, Level, Screen, Entities)} and {@link CatSystem#process(Cat, Position, Level, Position, Screen)}.
     * 
     * @param pos position component
     * @param screen terminal to write the characters glyph at its position
     */
    @Removed
    @None(Moving.class)
    @All({ Position.class, Render.class })
    final void removeNonMoving(Position pos, @Singleton Screen screen) {
        screen.drawChar(pos.x, pos.y, ' ');
    }

}
