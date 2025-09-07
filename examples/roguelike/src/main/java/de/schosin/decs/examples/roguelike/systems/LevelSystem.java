package de.schosin.decs.examples.roguelike.systems;

import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.One;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.examples.roguelike.Main;
import de.schosin.decs.examples.roguelike.components.Pickup;
import de.schosin.decs.examples.roguelike.components.Position;
import de.schosin.decs.examples.roguelike.components.Wall;
import de.schosin.decs.examples.roguelike.utils.Entities;
import de.schosin.decs.examples.roguelike.utils.Level;

/**
 * System for managing the {@link Level} value using {@link Inserted} and {@link Removed}. <br />
 * That {@link Level} is then used by the {@link InputSystem} for a simple collision check and picking up coins.
 */
public class LevelSystem {

    /**
     * Will be called whenever an entity that has a {@link Position} and a {@link Wall} component is created or modified. <br />
     * In this project that means whenever {@link Entities#createPlayer(int, int)} is called.
     * 
     * @param ref a reference to that entity that can be used to delete or modify it
     * @param pos Postion component of the entity
     * @param level level initialized in {@link Main}
     */
    @Inserted
    @All({ Position.class, Wall.class })
    final void addWall(Entity entity, Position pos, @Value("level") Level level) {
        level.set(pos.x, pos.y, Level.WALL, entity.createRef());
    }

    /**
     * Will be called whenever an entity that has a {@link Position} and a {@link Pickup} component is created or modified.
     * In this project that means whenever {@link Entities#createLevel(int, int)} is called.
     * 
     * @param ref a reference to that entity that can be used to delete or modify it
     * @param pos Postion component of the entity
     * @param level level initialized in {@link Main}
     */
    @Inserted
    @All({ Position.class, Pickup.class })
    final void addPickup(Entity entity, Position pos, Pickup pickup, @Value("level") Level level) {
        level.set(pos.x, pos.y, pickup.glyph, entity.createRef());
    }

    /**
     * Called whenever a wall or coin entity is removed.
     * In this project that means whenever {@link Entities#createLevel(int, int)} is called.
     * 
     * @param pos Postion component of the entity
     * @param level level initialized in {@link Main}
     */
    @Removed
    @All(Position.class)
    @One({ Wall.class, Pickup.class })
    final void removeLevelEntity(Position pos, @Value("level") Level level) {
        level.remove(pos.x, pos.y);
    }

}
