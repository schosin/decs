package de.schosin.decs.codegen.system.methods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.ManifestUtils;
import de.schosin.decs.codegen.utils.ParameterData;
import de.schosin.decs.codegen.utils.ParameterData.SystemParameterData;
import de.schosin.decs.codegen.utils.ParameterData.UtilityParameter;

/**
 * Sealed interface for annotated system methods.
 */
public sealed interface SystemMethod extends ProcessorMethod {

    static SystemMethod readMetadata(BufferedReader reader, AbstractGenerator generator) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case SystemProcessorMethod.TYPE -> SystemProcessorMethod.readMetadata(reader);
            case EntityProcessorMethod.TYPE -> EntityProcessorMethod.readMetadata(reader, generator);
            default -> throw new IllegalArgumentException("Unknown system method type '%s'. Perform a clean build.".formatted(type));
        };
    }

    /**
     * Describes a {@code @SystemProcessor} method.
     */
    record SystemProcessorMethod(ExecutableElement method, String methodName, boolean modifying, List<SystemParameterData> parameters) implements SystemMethod {

        static final String TYPE = "SYSTEM_PROCESSOR";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(methodName).append(System.lineSeparator());
            writer.append(modifying ? "true" : "false").append(System.lineSeparator());

            writer.append(String.valueOf(parameters.size())).append(System.lineSeparator());
            for (var parameter : parameters) {
                parameter.writeMetadata(writer);
            }
        }

        static SystemProcessorMethod readMetadata(BufferedReader reader) throws IOException {
            var methodName = reader.readLine();
            var modifying = "true".equals(reader.readLine());

            var count = Integer.parseInt(reader.readLine());
            var parameters = new ArrayList<SystemParameterData>(count);
            for (int i = 0; i < count; i++) {
                parameters.add(SystemParameterData.readMetadata(reader));
            }

            return new SystemProcessorMethod(null, methodName, modifying, parameters);
        }

    }

    /**
     * Describes a {@code @EntityProcessor} method.
     */
    record EntityProcessorMethod(ExecutableElement method, String methodName, CompositionData composition, List<ParameterData> parameters) implements SystemMethod {

        static final String TYPE = "ENTITY_PROCESSOR";

        public boolean modifying() {
            // Only modifying methods (@Archetype or @Transmute) exist currently, so no further checks needd
            return parameters.stream().anyMatch(parameter -> parameter instanceof UtilityParameter);
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(methodName).append(System.lineSeparator());
            ManifestUtils.writeCompositionData(composition, writer);

            writer.append(String.valueOf(parameters.size())).append(System.lineSeparator());
            for (var parameter : parameters) {
                parameter.writeMetadata(writer);
            }
        }

        static EntityProcessorMethod readMetadata(BufferedReader reader, AbstractGenerator generator) throws IOException {
            var methodName = reader.readLine();
            var compositionData = ManifestUtils.readCompositionData(reader, "@EntityProcessor '%s'".formatted(methodName));

            var count = Integer.parseInt(reader.readLine());
            var parameters = new ArrayList<ParameterData>(count);
            for (int i = 0; i < count; i++) {
                parameters.add(ParameterData.readMetadata(reader));
            }

            return new EntityProcessorMethod(null, methodName, compositionData, parameters);
        }

    }

}