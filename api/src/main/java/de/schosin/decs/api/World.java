package de.schosin.decs.api;

import java.util.Set;

import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;
import de.schosin.decs.api.annotations.utils.Archetype;
import de.schosin.decs.api.builder.WorldBuilder;
import de.schosin.decs.api.entities.EntityRef;
import de.schosin.decs.api.utils.collections.EntityBag;
import de.schosin.decs.values.Values;

/**
 * Main API for working with this library. <br />
 * To create an instance, use {@link #builder()} to obtain a builder and call {@link WorldBuilder#build()} after configuring the world.
 * 
 * <p>
 * This library builds on a declarative approach using annotation processing to generate optimized code. <br />
 * As such most common ECS tasks are either declared while {@link WorldBuilder building the world}, or in processors (see below). <br />
 * Common abstractions in other libraries such as component mappers are absent in this library on purpose.
 * 
 * <p>
 * This library requires the use of the annotation processor "decs-codegen".
 * 
 * <h3>Create entities</h3>
 * 
 * To create entities, you must define archetypes for them. <br />
 * Create an interface that contains one or more abstract methods annotated with {@link Archetype @Archetype}.
 * 
 * <p>
 * The parameters of the methods will be the components of the entities created by them. <br />
 * The return type of the methods can be either void or int (id of entity).
 * 
 * <p>
 * Use {@link #getUtility(Class)} to obtain an instance of your interface and call your methods to create entities.
 * 
 * {@snippet:
 * var world = World.builder().build();
 * MyArchetype archetype = world.createArchetype(MyArchetype.class);
 * int entityId = archetype.create(new Position(), new Velocity());
 * 
 * ...
 * 
 * abstract class MyArchetype {
 *     @Archetype
 *     abstract void create(int count);
 *     
 *     @EntityInitializer
 *     final void create(int index, Position position, Velocity velocity) {
 *         // initialize components
 *     }
 * }
 * }
 * 
 * <p>
 * Alternatively you can also inject utilities into processors, or declare @Archetype methods in systems directly. <br />
 * See the documentation for {@link EntityProcessor @EntityProcessor} and {@link SystemProcessor @SystemProcessor}.
 * 
 * <h3>Processors (Systems)</h3>
 *  
 * Processors are declared when building a world.
 * See the documentation for {@link WorldBuilder}.
 * 
 * <h3>Values</h3>
 * 
 * The method {@link #getValues()} returns a generated type containing fields for all usages of {@link Value @Value} in the sources. <br />
 * Use this generated type to inject mutable values such as a {@code delta} into systems. <br />
 * {@link Value @Value} can be used as a meta-annotation, see its documentation for an example.
 * 
 * <p>
 * If {@link Value @Value} is not used no type will be generated and referencing the return type of {@link #getValues()} will cause a compilation error. <br />
 * For this to function properly make sure "decs-values" is not on the classpath.
 */
public interface World {

    /**
     * Returns a builder for a new world.
     */
    static WorldBuilder builder() {
        return WorldBuilder.create();
    }

    /**
     * Gets an instance of a utility interface described by the class argument. <br />
     * Utilities are interfaces that only contain {@link de.schosin.decs.api.annotations.utils utility annotations}.
     * 
     * @param <T> type of utility
     * @param clazz class of utility 
     * @return instance of the utility
     * @throws InvalidUtilityException when implementation not found or not instantiable
     */
    <T> T getUtility(Class<T> clazz);

    /**
     * Returns the singleton of this type set with {@link WorldBuilder#singleton(Object)}.
     * 
     * @param <T> type of singleton
     * @param clazz {@link Class} of singleton
     * @return singleton instance or null if not found
     */
    <T> T getSingleton(Class<T> clazz);

    /**
     * Returns the values instance that holds the values being accessed with {@link Value @Value} in processors.
     * 
     * <p>
     * This method can only be used when {@link Value @Value} was used, as "decs-codegen" generates a compile-time type. <br />
     * Make sure "ecs-values" is not on the classpath to avoid issues with type clashes.
     * 
     * <p>
     * This type will be generated based on present {@link Value @Value} annotations in the source files. <br />
     * If {@link Value @Value} is not used, the {@link Values} type won't be generated and dereferencing that type will cause compile time errors.
     * 
     * @return values instance
     */
    Values getValues();
    
    /**
     * Returns a new entity bag for storing {@link EntityRef} instances.
     * 
     * <p>
     * The returned collection implements {@link Set Set<EntityRef>} and automatically removes entities that were deleted. <br />
     * By calling {@link EntityBag#free()}, the instance can be returned to the world and reused. 
     * 
     * @return entity set
     */
    EntityBag createEntityBag();

    /**
     * Processes the world. 
     * 
     * <p>
     * Will run all systems as well as maintenance tasks in between systems. <br />
     * Maintenance tasks include flushing creation, modification and deletion of entities.
     */
    void process();

}
