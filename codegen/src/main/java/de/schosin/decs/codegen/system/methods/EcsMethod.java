package de.schosin.decs.codegen.system.methods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

import javax.lang.model.element.ExecutableElement;

import de.schosin.decs.codegen.system.methods.CallbackMethod.InsertedMethod;
import de.schosin.decs.codegen.system.methods.CallbackMethod.RemovedMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod.EntityProcessorMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod.SystemProcessorMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.ArchetypeMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.CountMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.TransmuteMethod;
import de.schosin.decs.codegen.utils.AbstractGenerator;

/**
 * Base interface for annotated methods.
 */
public sealed interface EcsMethod permits ProcessorMethod, UtilityMethod {

    ExecutableElement method();

    String methodName();

    void writeMetadata(Writer writer) throws IOException;

    static EcsMethod readMetadata(BufferedReader reader, AbstractGenerator generator) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case SystemProcessorMethod.TYPE -> SystemProcessorMethod.readMetadata(reader);
            case EntityProcessorMethod.TYPE -> EntityProcessorMethod.readMetadata(reader, generator);
            case InsertedMethod.TYPE -> InsertedMethod.readMetadata(reader, generator);
            case RemovedMethod.TYPE -> RemovedMethod.readMetadata(reader, generator);
            case ArchetypeMethod.TYPE -> ArchetypeMethod.readMetadata(reader);
            case TransmuteMethod.TYPE -> TransmuteMethod.readMetadata(reader);
            case CountMethod.TYPE -> CountMethod.readMetadata(reader, generator);
            default -> throw new IllegalArgumentException("Unknown method type '%s'. Perform a clean build.".formatted(type));
        };
    }

}