package de.schosin.decs.examples.roguelike.systems;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Singleton;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.examples.roguelike.Main;
import de.schosin.decs.examples.roguelike.components.Cat;
import de.schosin.decs.examples.roguelike.components.Player;
import de.schosin.decs.examples.roguelike.components.Position;
import de.schosin.decs.examples.roguelike.singletons.Screen;
import de.schosin.decs.examples.roguelike.utils.Entities;
import de.schosin.decs.examples.roguelike.utils.Level;

/**
 * System for processing user input provided passed in as a {@link Value @Value}. <br />
 * The value of the input is set in the main loop in {@link Main}.
 */
public class InputSystem {

    /**
     * Updates the position of the player entity based on the {@code input parameter} and the current {@link Level level}.
     * Will be called for all entities that have a {@link Player} and a {@link Position} component.
     * 
     * @param input input, set by {@link Main}
     * @param pos position component of the entity
     * @param player player component for the score
     * @param level current level to check where the user moves to
     * @param screen terminal for updating the players glyph
     */
    @All({ Player.class, Position.class })
    @EntityProcessor
    final void process(@Value("input") String input, Position pos, Player player, @Value("level") Level level, @Singleton Screen screen, @Utility Entities entities) {
        var x = pos.x;
        var y = pos.y;

        switch (input) {
            case "w" -> y--;
            case "a" -> x--;
            case "s" -> y++;
            case "d" -> x++;
            case null, default -> {
                return;
            }
        }

        var c = level.get(x, y);
        switch (c) {
            case Level.SPACE -> {
                // Update position component
                screen.drawChar(pos.x, pos.y, ' '); // clear old position

                pos.x = x;
                pos.y = y;
            }
            case Level.WALL -> {
                // movement blocked
            }
            case Level.COIN -> {
                // Update position component
                screen.drawChar(pos.x, pos.y, ' '); // clear old position

                pos.x = x;
                pos.y = y;

                // Remove coin entity and increment current score
                level.remove(x, y).delete();
                player.score++;
            }
            case Level.CAT -> {
                // Update position component
                screen.drawChar(pos.x, pos.y, ' '); // clear old position

                pos.x = x;
                pos.y = y;

                // Remove cat entity and increment current score by the cats score
                var cat = level.remove(x, y);
                player.score += cat.getComponent(Cat.class).score;

                cat.delete();
            }
            default -> throw new IllegalStateException("Unknown level character at (%d, %d): %s".formatted(x, y, c));
        }
    }

    /**
     * Sets the {@code @Value("playerPos"} when the player is inserted.
     */
    @All({ Player.class, Position.class })
    @Inserted
    final void setPlayerPos(Position pos, World world) {
        world.getValues().playerPos = pos;
    }

    /**
     * Removes the {@code @Value("playerPos"} when the player is removed.
     */
    @All(Player.class)
    @Removed
    final void removePlayerPos(World world) {
        world.getValues().playerPos = null;
    }

}
