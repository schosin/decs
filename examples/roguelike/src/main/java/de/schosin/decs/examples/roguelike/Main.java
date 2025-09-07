package de.schosin.decs.examples.roguelike;

import java.io.IOException;

import org.jline.terminal.Terminal;

import de.schosin.decs.api.World;
import de.schosin.decs.api.annotations.Singleton;
import de.schosin.decs.api.annotations.Utility;
import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.composition.None;
import de.schosin.decs.api.annotations.composition.One;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.Inserted;
import de.schosin.decs.api.annotations.system.Removed;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.annotations.utils.Count;
import de.schosin.decs.api.annotations.utils.Transmute;
import de.schosin.decs.api.entities.Entity;
import de.schosin.decs.api.entities.EntityArchetype;
import de.schosin.decs.examples.roguelike.singletons.Screen;
import de.schosin.decs.examples.roguelike.systems.CatSystem;
import de.schosin.decs.examples.roguelike.systems.InputSystem;
import de.schosin.decs.examples.roguelike.systems.LevelSystem;
import de.schosin.decs.examples.roguelike.systems.RenderSystem;
import de.schosin.decs.examples.roguelike.utils.Entities;
import de.schosin.decs.examples.roguelike.utils.Level;

/**
 * <h4>Note:</h4>
 * 
 * <p>
 * If the project contains any compile time errors, check if annotation processing is enabled and {@code decs-codegen} is used as an annotation processor. <br />
 * Sometimes a full clean build might be required, especially when removing types that were referenced in the generated code, such as components or systems. <br />
 * 
 * When using an IDE, consider not importing or closing the modules {@code codegen} and {@code values}, as these can cause problems depending on the IDE. <br />
 * To use them, run {@code mvn clean install -am -pl codegen -DskipTests} and refresh the project in the IDE.
 * 
 * <h3>Description:</h3>
 * 
 * <p>
 * In this example you play as a home owner that has dropped all of his money in his 2000m² room. <br />
 * With you in that room are vicious, but drunken cats that try to steal your hard earned cash. <br />
 * Your task is to pick up all your money and punish your feline companions for stealing from you. <br />
 * The reward is a clean room you can run around in until you get bored and (Q)uit.
 * 
 * <h4>Main classes:</h4>
 * 
 * <p>
 * To get a better idea of how the library works, start with this class and then move to the types used in here. <br />
 * Also see the next section for terms used in this example, as well as in the library itself.
 * 
 * <p>
 * <ul>
 *  <li><b>{@link Entities}:</b> {@link Utility @Utility} for creating entities</li>
 *  <li><b>{@link LevelSystem}:</b> System for managing the {@link Level} value</li>
 *  <li><b>{@link Level}:</b> Holds data about the state of the current level, as well as references to the entities</li>
 *  <li><b>{@link InputSystem}:</b> System for processing user input, which is held in the generated Values type</li>
 *  <li><b>{@link CatSystem}:</b> System for the "AI" of the main antagonists of this game</li>
 *  <li><b>{@link RenderSystem}:</b> System for rendering the game state to the terminal</li>
 *  <li><b>{@link Screen}:</b> Wrapper around {@link Terminal} registered as a {@link Singleton @Singleton}</li>
 *  <li><b>{@link Values}:</b> Generated type based on usage of {@link Value @Value} to support injection of mutable and primitive parameters into processors</li>
 * </ul>
 * 
 * <h3>Library:</h3>
 * 
 * <h4>Terms</h4>
 * 
 * <p>
 * <ul>
 *  <li><b>Singleton:</b> 
 *      An instance of an arbitraty type that can be injected into processors and callbacks using {@link Singleton @Singleton}
 *  </li>
 *  <li><b>Value:</b> 
 *      An instance of an arbitraty type (including primitives) that can be injected into processors and callbacks using {@link Value @Value}
 *  </li>
 *  <li><b>Composition:</b> 
 *      The term composition means the annotations {@link All @All}, {@link One @One} and {@link None @None}. <br />
 *      These annotations can be used together with {@link EntityProcessor}, {@link Inserted}, {@link Removed} and {@link Count} on methods, or on types containing these methods. <br />
 *      When the type and the method is annotated, the annotations on the method take precedence. <br />
 *      These annotations can be freely combined with one exception.<br />
 *      <br />
 *      Use {@link All} without parameters to select all entities. No other composition annotations can be used.
 *      Use {@link All} with parameters to select all entites that have all of those components.
 *      Use {@link One} with parameters to select all entities that have atleast one of those components. This annotated can be used multiple times.
 *      Use {@link None} with parameters to select all entites that have none of those components. <br />
 *      <br />
 *      See {@link de.schosin.decs.api.annotations.composition} and the JavaDoc of those annotations for more information, including the support for meta annotations.
 *  </li>
 *  <li><b>Entity:</b>
 *      The entity is defined by its components and can be accessed by injecting an {@code int} or {@link Entity} into {@link EntityProcessor @EntityProcessor} or {@link Inserted @Inserted} methods.
 *  </li>
 *  <li><b>System:</b> 
 *      A system is any class that contains atleast one processor or callback method.
 *      Systems are processed in the order they are added when building the world.
 *      Systems can also declare utility methods.
 *  </li>
 *  <li><b>Processor methods:</b> 
 *      Means {@link SystemProcessor @SystemProcessor} and {@link EntityProcessor @EntityProcessor}, the methods that are run every time {@link World#process()} is called. <br />
 *      When a system is processed, these methods will be run in the same order as in the sources. 
 *  </li>
 *  <li><b>Callback methods:</b> 
 *      Means {@link Inserted @Inserted} and {@link Removed @Removed}, the methods that are called when matching entities are inserted or removed. <br />
 *      Callback methods targeting the same composition are generally run in the order as the system order, followed by the order in the source, but that is not guaranteed. <br />
 *      For callback methods targeting different compositions the order is undefined. 
 *  </li>
 *  <li><b>Utilities:</b>
 *      A utility is a type that only contains {@link de.schosin.decs.api.annotations.utils utility methods} and can be injected using {@link Utility @Utility}. <br />
 *      Utilities can also be obtained with {@link World#getUtility(Class)} to initialize a world by creating entities with {@link Archetype @Archetype} methods. 
 *  </li>
 *  <li><b>Archetype:</b>
 *      An archetype is both the underlying memory layout, and the utility annotation {@link Archetype @Archetype} used to create entities. <br />
 *      Archetypes are defined by their components and contain all entities that have the exact same components. <br />
 *      When using compositions (see above), they are matched against archetypes instead of entities, which reduces the processing cost of using compositions, as well as creating, modifying and deleting entities.
 *  </li>
 *  <li><b>Transmute:</b> 
 *      Transmute is the utility annotation {@link Transmute} used to add and remove components from entities.
 *  </li>
 * </ul>
 * 
 * <h4>Technical details</h4>
 * 
 * <p>
 * To understand the inner workings of the library, it is probably best to just add a breakpoint in a processor or callback method and looking at the stacktrace. <br />
 * Thanks to the annotation processing, these stack traces are rather short and the steps from {@link World#process()} are mostly basic java. <br />
 * <br />
 * To understand how the creation of entities work, you can place a breakpoint in any of the public methods in {@link Entities} and step into the generated code. <br />
 * It's best to first step over any methods of {@link EntityArchetype} to understand the high-level implementation. <br />
 * <br />
 * Alternatively all generatd classes can be looked at as well to understand how they work. <br />
 * The generated classes in the {@link de.schosin.decs.examples.roguelike roguelike package} are based off the user code in this module. <br />
 * The generated classes in the {@link de.schosin.decs.values values package} contain the required metadata about components, systems, utilities and values to enable the zero* reflection 
 */
public class Main {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 25;

    private static final String QUIT = "q";

    public static void main(String[] args) throws IOException {
        // Singleton for rendering the world to the terminal
        var screen = new Screen(WIDTH, HEIGHT + 1);

        // Create an instance of the world
        var world = World.builder()
                .singleton(screen) // pass singleton
                .add(LevelSystem.class, InputSystem.class, CatSystem.class, RenderSystem.class) // add system types
                .build();

        // The type Values is generated when @Value is used in the project.
        // Intended to be used as injectable parameters, such as the level, the user input (probably not needed in LibGDX) or maybe a `@Value("delta") float delta`
        // See the javadoc of @Value for more information, including on hwo to create a meta-annotation such as @Delta
        var values = world.getValues();
        values.level = new Level(WIDTH, HEIGHT);

        // Utilities are types that contain utility annotations (de.schosin.decs.api.annotations.utils)
        // Utilities include @Archetype, which is the only way to create entities
        var entities = world.getUtility(Entities.class);
        entities.createPlayer(screen.getCenterX(), screen.getCenterY());
        entities.createLevel(screen.getWidth(), screen.getHeight() - 1);
        entities.createCats(4, screen.getWidth(), screen.getHeight() - 1);

        // Simple game loop
        while (true) {
            // Runs the system and processes any needed changes.
            // Systems are run in the order they are added during world building.
            world.process();

            // Prompt for user input (blocking) and write it to Values
            // Used by InputSystem with @Value("input")
            values.input = screen.prompt("WASD to move, Space to wait, Q to quit");

            // Quit if "q" is pressed
            if (QUIT.equals(values.input)) {
                break;
            }
        }
    }

}
