package de.schosin.decs.codegen.system.methods;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.utils.ManifestUtils;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;
import de.schosin.decs.codegen.utils.ParsedType;

import javax.lang.model.element.ExecutableElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public sealed interface UtilityMethod extends EcsMethod {

    static UtilityMethod readMetadata(BufferedReader reader) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case ArchetypeMethod.TYPE -> ArchetypeMethod.readMetadata(reader);
            case TransmuteMethod.TYPE -> TransmuteMethod.readMetadata(reader);
            case CountMethod.TYPE -> CountMethod.readMetadata(reader);
            default ->
                    throw new IllegalArgumentException("Unknown utility method type '%s'. Perform a clean build.".formatted(type));
        };
    }

    /**
     * Describes an {@code @Archetype} method.
     */
    record ArchetypeMethod(ExecutableElement method, ExecutableElement initializer, String methodName,
                           String initializerName, boolean returnsEntityRefs, List<Parameter> parameters,
                           List<ComponentParameter> enumComponents,
                           List<ComponentParameter> components) implements UtilityMethod {

        static final String TYPE = "ARCHETYPE";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(methodName).append(System.lineSeparator());
            writer.append(initializerName).append(System.lineSeparator());
            writer.append(returnsEntityRefs ? "true" : "false").append(System.lineSeparator());

            writer.append(String.valueOf(parameters.size())).append(System.lineSeparator());
            for (var parameter : parameters) {
                parameter.writeMetadata(writer);
            }

            writer.append(String.valueOf(enumComponents.size())).append(System.lineSeparator());
            for (var parameter : enumComponents) {
                parameter.writeMetadata(writer);
            }

            writer.append(String.valueOf(components.size())).append(System.lineSeparator());
            for (var parameter : components) {
                parameter.writeMetadata(writer);
            }
        }

        static ArchetypeMethod readMetadata(BufferedReader reader) throws IOException {
            var methodName = reader.readLine();
            var initializerName = reader.readLine();
            var returnsEntityRefs = "true".equals(reader.readLine());

            var parameterCount = Integer.parseInt(reader.readLine());
            var parameters = new ArrayList<Parameter>(parameterCount);
            for (int i = 0; i < parameterCount; i++) {
                parameters.add(Parameter.readMetadata(reader));
            }

            var enumCount = Integer.parseInt(reader.readLine());
            var enumComponents = new ArrayList<ComponentParameter>(enumCount);
            for (int i = 0; i < enumCount; i++) {
                reader.readLine(); // discard type
                enumComponents.add(ComponentParameter.readMetadata(reader));
            }

            var componentCount = Integer.parseInt(reader.readLine());
            var components = new ArrayList<ComponentParameter>(componentCount);
            for (int i = 0; i < componentCount; i++) {
                reader.readLine(); // discard type
                components.add(ComponentParameter.readMetadata(reader));
            }

            return new ArchetypeMethod(null, null, methodName, initializerName, returnsEntityRefs, parameters, enumComponents, components);
        }

    }

    /**
     * Describes a {@code @Transmute} method.
     */
    record TransmuteMethod(ExecutableElement method, ExecutableElement initializer, String methodName,
                           String initializerName, ClassName entityParam, List<Parameter> parameters,
                           List<ComponentParameter> enumComponents, List<ComponentParameter> components,
                           List<TypeName> remove) implements UtilityMethod {

        static final String TYPE = "TRANSMUTE";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(methodName).append(System.lineSeparator());
            writer.append(initializerName).append(System.lineSeparator());
            writer.append(entityParam.toString()).append(System.lineSeparator());

            writer.append(String.valueOf(parameters.size())).append(System.lineSeparator());
            for (var parameter : parameters) {
                parameter.writeMetadata(writer);
            }

            writer.append(String.valueOf(enumComponents.size())).append(System.lineSeparator());
            for (var parameter : enumComponents) {
                parameter.writeMetadata(writer);
            }

            writer.append(String.valueOf(components.size())).append(System.lineSeparator());
            for (var parameter : components) {
                parameter.writeMetadata(writer);
            }

            writer.append(String.valueOf(remove.size())).append(System.lineSeparator());
            for (var parameter : remove) {
                writer.append(parameter.toString()).append(System.lineSeparator());
            }
        }

        static TransmuteMethod readMetadata(BufferedReader reader) throws IOException {
            var methodName = reader.readLine();
            var initializerName = reader.readLine();
            var entityParam = ParsedType.parse(reader.readLine()).getClassName();

            var parameterCount = Integer.parseInt(reader.readLine());
            var parameters = new ArrayList<Parameter>(parameterCount);
            for (int i = 0; i < parameterCount; i++) {
                parameters.add(Parameter.readMetadata(reader));
            }

            var enumCount = Integer.parseInt(reader.readLine());
            var enumComponents = new ArrayList<ComponentParameter>(enumCount);
            for (int i = 0; i < enumCount; i++) {
                reader.readLine(); // discard type
                enumComponents.add(ComponentParameter.readMetadata(reader));
            }

            var componentCount = Integer.parseInt(reader.readLine());
            var components = new ArrayList<ComponentParameter>(componentCount);
            for (int i = 0; i < componentCount; i++) {
                reader.readLine(); // discard type
                components.add(ComponentParameter.readMetadata(reader));
            }

            var removeCount = Integer.parseInt(reader.readLine());
            var remove = new ArrayList<TypeName>(removeCount);
            for (int i = 0; i < removeCount; i++) {
                remove.add(ParsedType.parse(reader.readLine()).getTypeName());
            }

            return new TransmuteMethod(null, null, methodName, initializerName, entityParam, parameters, enumComponents, components, remove);
        }

    }

    /**
     * Describes a {@code @Count} method.
     */
    record CountMethod(ExecutableElement method, String methodName,
                       CompositionData composition) implements UtilityMethod {

        static final String TYPE = "COUNT";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(methodName).append(System.lineSeparator());
            ManifestUtils.writeCompositionData(composition, writer);
        }

        public static CountMethod readMetadata(BufferedReader reader) throws IOException {
            var methodName = reader.readLine();
            var composition = ManifestUtils.readCompositionData(reader, "@Count '%s'".formatted(methodName));

            return new CountMethod(null, methodName, composition);
        }

    }

}
