package de.schosin.decs.examples.roguelike.utils;

import java.awt.Component;
import java.util.random.RandomGenerator;

import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.EntityInitializer;
import de.schosin.decs.examples.roguelike.components.Cat;
import de.schosin.decs.examples.roguelike.components.Moving;
import de.schosin.decs.examples.roguelike.components.Pickup;
import de.schosin.decs.examples.roguelike.components.Player;
import de.schosin.decs.examples.roguelike.components.Position;
import de.schosin.decs.examples.roguelike.components.Render;
import de.schosin.decs.examples.roguelike.components.Wall;
import de.schosin.decs.examples.roguelike.singletons.Screen;
import de.schosin.decs.examples.roguelike.systems.RenderSystem;

/**
 * This type bundles all methods to create entities. <br />
 * To keep the public API simple, the technical implementation is hidden by using the {@code protected} access modifier.
 * 
 * <p>
 * To create entities, the {@link Archetype @Archetype} annotation must be used. <br />
 * The annotated method must declare the {@code count} of entities to create as a first argument. <br />
 * After that comes an optional list of arbitrary arguments that can be used to parameterize the initialization logic. <br />
 * After that comes an optional list of enum {@link Component components}, which will be added to the entity.
 * 
 * <p>
 * Every {@link Archetype @Archetype} method must be following by a {@link EntityInitializer @EntityInitializer method}. <br />
 * That method must start with the same parameters as the archetype method, but rename the first argument from {@code count} to {@code index}. <br />
 * After that comes an optional list of non-enum {@link Component components}, wich will be added the entity.
 * For {@link #createPlayer(int, Screen, Player, Moving)} that is {@link #createPlayer(int, Screen, Player, Moving, Position, Render)}.
 * 
 * <p>
 * Archetypes enforce that atleast one component (enum or non-enum) must be added to an entity. <br />
 * Creating entities without any components is not supported.
 */
public abstract class Entities {

    private static final RandomGenerator RNG = RandomGenerator.getDefault();

    /**
     * Create player at the center of the screen. 
     */
    public final void createPlayer(int x, int y) {
        createPlayer(1, x, y, Moving.MOVING);
    }

    /**
     * Archetype for the player entity.
     * 
     * @param count required argument, indicating the number of entiites to create
     * @param x arbitrary argument, used to set the Position component
     * @param y arbitrary argument, used to set the Position component
     * @param moving enum component to indicate the entity is capable of movement, used by {@link RenderSystem}
     */
    @Archetype
    protected abstract void createPlayer(int count, int x, int y, Moving moving);

    /**
     * Initializer for the player entity. <br />
     * Will be called {@code count} times when {@link #createPlayer(int, Screen, Player, Moving)} is called.
     * 
     * @param index required argument, will have values from {@code 0..count-1}
     * @param x matches archetype method
     * @param y matches archetype method
     * @param moving matches archetype method
     * @param player Player component of the player
     * @param pos Position component of the player
     * @param render Render component of the player
     */
    @EntityInitializer
    protected final void createPlayer(int index, int x, int y, Moving moving, Player player, Position pos, Render render) {
        pos.x = x;
        pos.y = y;

        render.glyph = '@';
    }

    /**
     * Creates the level entities.
     */
    public void createLevel(int width, int height) {
        // Outer walls
        createWalls(width - 2, 1, 0, 1, 0, Wall.WALL);
        createWalls(width - 2, 1, height - 1, 1, 0, Wall.WALL);
        createWalls(height, 0, 0, 0, 1, Wall.WALL);
        createWalls(height, width - 1, 0, 0, 1, Wall.WALL);

        // Create some coins
        var centerX = width / 2;
        var centerY = height / 2;

        for (int x = 3, xs = width - 3; x < xs; x++) {
            for (int y = 3, ys = height - 3; y < ys; y++) {
                if ((x == centerX || Math.abs(centerX - x) == centerX) && (y == centerY || Math.abs(centerY - y) == centerY)) {
                    continue;
                }

                if (RNG.nextInt(20) == 0) {
                    createPickup(1, x, y, Pickup.COIN);
                }
            }
        }
    }

    /**
     * Archetype for wall entities.
     * 
     * @param count required argument, indicating the number of entiites to create
     * @param x arbitrary argument, used to calculate the position of the wall
     * @param y arbitrary argument, used to calculate the position of the wall
     * @param xOffset arbitrary argument, used to calculate the position of the wall
     * @param yOffset arbitrary argument, used to calculate the position of the wall
     * @param wall enum component to indicate the entity is a wall
     */
    @Archetype
    protected abstract void createWalls(int count, int x, int y, int xOffset, int yOffset, Wall wall);

    /**
     * Initializer for wall entities. <br />
     * Will be called {@code count} times when {@link #createWalls(int, int, int, int, int, Wall)} is called.
     * 
     * @param index required argument, will have values from {@code 0..count-1}
     * @param x matches archetype method
     * @param y matches archetype method
     * @param xOffset matches archetype method
     * @param yOffset matches archetype method
     * @param wall matches archetype method
     * @param pos Position component of the wall
     * @param render Render component of the wall
     */
    @EntityInitializer
    protected final void createWalls(int index, int x, int y, int xOffset, int yOffset, Wall wall, Position pos, Render render) {
        pos.x = x + index * xOffset;
        pos.y = y + index * yOffset;

        render.glyph = Level.WALL;
    }

    /*
     * Archetype for coins.
     * 
     * @param count required argument, indicating the number of entiites to create
     * @param x arbitrary argument, used for the position component
     * @param y arbitrary argument, used for the position component
     * @param pickup enum component to indicate the entity is a pickup
     */
    @Archetype
    protected abstract void createPickup(int count, int x, int y, Pickup pickup);

    /**
     * Initializer for coins. <br />
     * Will be called {@code count} times when {@link #createPickup(int, int, int, Wall, Pickup)} is called.
     * 
     * @param index required argument, will have values from {@code 0..count-1}
     * @param x matches archetype method
     * @param y matches archetype method
     * @param pickup matches archetype method
     * @param pos Position component of the wall
     * @param render Render component of the wall
     */
    @EntityInitializer
    protected final void createPickup(int index, int x, int y, Pickup pickup, Position pos, Render render) {
        pos.x = x;
        pos.y = y;

        render.glyph = pickup.glyph;
    }

    public void createCats(int count, int width, int height) {
        for (int i = 0; i < count; i++) {
            int x;
            int y;

            do {
                x = 5 + RNG.nextInt(width - 5);
                y = 5 + RNG.nextInt(height - 5);
            } while (x == width / 2 && y == height / 2);

            createCat(1, x, y, Moving.MOVING);
        }
    }

    @Archetype
    protected abstract void createCat(int count, int x, int y, Moving moving);

    @EntityInitializer
    protected final void createCat(int index, int x, int y, Moving moving, Cat cat, Position pos, Render render) {
        pos.x = x;
        pos.y = y;

        render.glyph = Level.CAT;
    }

}
