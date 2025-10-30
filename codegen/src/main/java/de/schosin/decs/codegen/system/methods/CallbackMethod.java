package de.schosin.decs.codegen.system.methods;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.utils.ManifestUtils;
import de.schosin.decs.codegen.utils.ParameterData;

import javax.lang.model.element.ExecutableElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sealed interface for annotated system callback methods.
 */
public sealed interface CallbackMethod extends SystemMethod {

    CompositionData composition();

    @Override
    List<ParameterData> parameters();

    static CallbackMethod readMetadata(BufferedReader reader) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case InsertedMethod.TYPE -> InsertedMethod.readMetadata(reader);
            case RemovedMethod.TYPE -> RemovedMethod.readMetadata(reader);
            default -> throw new IllegalArgumentException("Unknown callback method type '%s'. Perform a clean build.".formatted(type));
        };
    }

    /**
     * Describes a {@code @Inserted} method.
     */
    record InsertedMethod(ExecutableElement method, String methodName, CompositionData composition, List<ParameterData> parameters) implements CallbackMethod {

        static final String TYPE = "INSERTED";

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

        static InsertedMethod readMetadata(BufferedReader reader) throws IOException {
            var methodName = reader.readLine();
            var compositionData = ManifestUtils.readCompositionData(reader, "@Inserted '%s'".formatted(methodName));

            var parameterCount = Integer.parseInt(reader.readLine());
            var parameters = new ArrayList<ParameterData>(parameterCount);
            for (int i = 0; i < parameterCount; i++) {
                parameters.add(ParameterData.readMetadata(reader));
            }

            return new InsertedMethod(null, methodName, compositionData, parameters);
        }

    }

    /**
     * Describes a {@code @Removed} method.
     */
    record RemovedMethod(ExecutableElement method, String methodName, CompositionData composition, List<ParameterData> parameters) implements CallbackMethod {

        static final String TYPE = "REMOVED";

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

        static RemovedMethod readMetadata(BufferedReader reader) throws IOException {
            var methodName = reader.readLine();
            var compositionData = ManifestUtils.readCompositionData(reader, "@Removed '%s'".formatted(methodName));

            var parameterCount = Integer.parseInt(reader.readLine());
            var parameters = new ArrayList<ParameterData>(parameterCount);
            for (int i = 0; i < parameterCount; i++) {
                parameters.add(ParameterData.readMetadata(reader));
            }

            return new RemovedMethod(null, methodName, compositionData, parameters);
        }

    }

}