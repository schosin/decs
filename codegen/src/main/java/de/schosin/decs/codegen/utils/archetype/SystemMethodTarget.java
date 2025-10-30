package de.schosin.decs.codegen.utils.archetype;

import de.schosin.decs.codegen.system.methods.SystemMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

/**
 * Describes the target of read and write operations of a {@link SystemMethod}.
 *
 * <h4>Read operations</h4>
 *
 * <p>
 * The targets are determined based on the methods a method could call or is known to call.
 * For @EntityProcessor methods annotated with @All (on the method or system), it is known that they read all archetypes.
 * For @EntityProcessor methods annotated with composition annotations, it is known that they read all archetypes that match the composition.
 *
 * <h4>Write operations</h4>
 *
 * <p>
 * When an @Archetype method is available (either within the system or a utility class), it is known that the system might write to the corresponding entity archetype.
 * When a @Transmute method is available, it is known that methods that declare BaseEntity parameters might write to archetypes that match their composition and the applied transmutation.
 * When a BaseEntity or Entity parameter is declared, it is known that that method might write to all archetypes that match the methods composition (delete).
 *
 * <h4>Runtime operations</h4>
 *
 * <p>
 * When the user obtains an EntityRef instance, write operations could happen anytime.
 * When the user deletes the entity, it could be a write operation on all reachable archetypes.
 * These write operations can target all
 *
 * <h4>Reachable & runtime archetypes
 *
 * <p>
 * A "reachable archetype" is every archetype that starts with an @Archetype entity and can be reached by applying any combination of @Transmute methods.
 * The number of reachable archetypes is limited and in most cases an upper bound of the actual archetypes used at runtime.
 *
 * <p>
 * To determine the exact number of runtime archetypes would require static code analysis and no use of code where source code is not available.
 * This will not be implemented anytime soon, and as such whenever a runtime write operation is performed, more costly synchronization is required.
 *
 * <p>
 * To avoid a performance impact for users not using EntityRef, users of the writing operations of EntityRef in parallel systems must declare doing so using @UnsafeWrites.
 * This annotation causes synchronization code to be generated for all code that is run in parallel with the annotated method or system.
 * When this annotation is used on a non-system method or type, synchronization code will be generated for all parallel systems.
 */
public sealed interface SystemMethodTarget permits ArchetypeTarget, TransmuteTarget, CompositeTarget {

    void writeMetadata(Writer writer) throws IOException;

    static SystemMethodTarget readMetadata(BufferedReader reader) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case ArchetypeTarget.TYPE -> ArchetypeTarget.readMetadata(reader);
            case TransmuteTarget.TYPE -> TransmuteTarget.readMetadata(reader);
            case CompositeTarget.TYPE -> CompositeTarget.readMetadata(reader);
            default -> throw new IllegalArgumentException("Unknown system method target type '%s'. Perform a clean build.".formatted(type));
        };
    }

}
