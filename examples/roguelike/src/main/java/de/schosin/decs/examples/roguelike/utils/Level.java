package de.schosin.decs.examples.roguelike.utils;

import de.schosin.decs.api.annotations.Singleton;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.entities.EntityRef;
import de.schosin.decs.examples.roguelike.Utils;

/**
 * Simple state holder for the level data.
 * 
 * <p>
 * Currently used as a {@link Value @Value}, but could just as well be used as a {@link Singleton @Singleton} in the current implementation. <br />
 * Think of {@link Value @Value} as a mutable alternative to {@link Singleton @Singleton} that also supports generics and primitive types.
 */
public final class Level {

    public static final char SPACE = ' ';
    public static final char WALL = '#';
    public static final char COIN = '$';
    public static final char CAT = 'c';

    /**
     * Holds the char of the entity at the given position {@code level[y][x]}. <br />
     * The char identifies the type of entity, which correspond to above constants.
     */
    private final char[][] level;

    /**
     * Holds the {@link EntityRef} for the entities at the given position {@code entities[y][x]}. <br />
     * These are used to {@link EntityRef#delete()} them whenever they are overwritten with {@link #set(int, int, char, EntityRef)} or {@link #remove(int, int)}.
     */
    private final EntityRef[][] entities;

    public Level(int width, int height) {
        this.level = Utils.createCharArray(width, height, SPACE);
        this.entities = new EntityRef[height][width];
    }

    public char get(int x, int y) {
        return this.level[y][x];
    }

    public EntityRef set(int x, int y, char c, EntityRef entity) {
        var existing = this.entities[y][x];

        this.level[y][x] = c;
        this.entities[y][x] = entity;

        return existing;
    }

    public EntityRef remove(int x, int y) {
        return set(x, y, SPACE, null);
    }

}
