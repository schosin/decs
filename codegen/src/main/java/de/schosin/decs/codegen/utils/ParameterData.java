package de.schosin.decs.codegen.utils;

import com.palantir.javapoet.*;
import com.palantir.javapoet.CodeBlock.Builder;
import de.schosin.decs.codegen.components.ComponentData;
import de.schosin.decs.codegen.components.ComponentData.InterfaceComponent;
import de.schosin.decs.codegen.entityarchetype.EntityArchetypeDataGenerator;
import de.schosin.decs.codegen.system.CompositionData;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.stream.Stream;

public sealed interface ParameterData {

    VariableElement parameter();

    String name();

    TypeName type();

    void entityAccessor(CodeBlock.Builder code, String entitiesArrayVar, String indexVar, CompositionData composition);

    void writeMetadata(Writer writer) throws IOException;

    static ParameterData readMetadata(BufferedReader reader) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case WorldParameter.TYPE -> WorldParameter.readMetadata(reader);
            case ValueParameter.TYPE -> ValueParameter.readMetadata(reader);
            case UtilityParameter.TYPE -> UtilityParameter.readMetadata(reader);
            case SingletonParameter.TYPE -> SingletonParameter.readMetadata(reader);
            case EntityIdParameter.TYPE -> EntityIdParameter.readMetadata(reader);
            case EntityParameter.TYPE -> EntityParameter.readMetadata(reader);
            case ComponentParameter.TYPE -> ComponentParameter.readMetadata(reader);
            default ->
                    throw new IllegalArgumentException("Unknown parameter type '%s'. Perform a clean build.".formatted(type));
        };
    }

    sealed interface FieldProvider extends ParameterData {

        FieldSpec fieldSpec();

        void fieldInit(CodeBlock.Builder code, String worldVar, String archetypeDataVar);

    }

    sealed interface LocalVariableProvider extends ParameterData {

        void localVariable(CodeBlock.Builder code, CompositionData composition);

    }

    sealed interface LoopInitProvider extends ParameterData {

        void loopInit(CodeBlock.Builder code, String indexVar, boolean inline);

    }

    sealed interface CleanUpProvider extends ParameterData {

        void cleanUp(CodeBlock.Builder code, boolean inline);

    }

    sealed interface SystemParameterData extends ParameterData {

        void systemAccessor(CodeBlock.Builder code);

        static SystemParameterData readMetadata(BufferedReader reader) throws IOException {
            var type = reader.readLine();
            return switch (type) {
                case WorldParameter.TYPE -> WorldParameter.readMetadata(reader);
                case ValueParameter.TYPE -> ValueParameter.readMetadata(reader);
                case UtilityParameter.TYPE -> UtilityParameter.readMetadata(reader);
                case SingletonParameter.TYPE -> SingletonParameter.readMetadata(reader);
                default ->
                        throw new IllegalArgumentException("Unknown parameter type '%s'. Perform a clean build.".formatted(type));
            };
        }

    }

    record WorldParameter(VariableElement parameter, String name) implements SystemParameterData {

        private static final String TYPE = "WORLD";

        @Override
        public ClassName type() {
            return Utils.WORLD;
        }

        @Override
        public void systemAccessor(Builder code) {
            code.add("_world");
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            code.add("_world");
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
        }

        public static WorldParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();
            return new WorldParameter(null, name);
        }

    }

    record ValueParameter(VariableElement parameter, String name, TypeName type,
                          String valueName) implements SystemParameterData, FieldProvider, LocalVariableProvider {

        private static final String TYPE = "VALUE";

        @Override
        public FieldSpec fieldSpec() {
            return FieldSpec.builder(Utils.VALUES, "_values", Modifier.PRIVATE, Modifier.FINAL).build();
        }

        @Override
        public void fieldInit(Builder code, String worldVar, String archetypeDataVar) {
            code.addStatement("this._values = %s.getValues()".formatted(worldVar));
        }

        @Override
        public void localVariable(Builder code, CompositionData composition) {
            code.addStatement("$1T %s = _values.%s".formatted(name, valueName), type);
        }

        @Override
        public void systemAccessor(Builder code) {
            code.add("_values.%s".formatted(valueName));
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            code.add(name);
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
            writer.append(type.toString()).append(System.lineSeparator());
            writer.append(valueName).append(System.lineSeparator());
        }

        public static ValueParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();
            var type = ParsedType.parse(reader.readLine()).getTypeName();
            var valueName = reader.readLine();

            return new ValueParameter(null, name, type, valueName);
        }

    }

    record UtilityParameter(VariableElement parameter, String name, ClassName type,
                            String fieldName) implements SystemParameterData, FieldProvider {

        private static final String TYPE = "UTILITY";

        public UtilityParameter(VariableElement parameter, String name, ClassName type) {
            this(parameter, name, type, Utils.decapitalize(type.simpleName()));
        }

        @Override
        public FieldSpec fieldSpec() {
            return FieldSpec.builder(type, fieldName, Modifier.PRIVATE, Modifier.FINAL).build();
        }

        @Override
        public void fieldInit(Builder code, String worldVar, String archetypeDataVar) {
            code.addStatement("this.%s = %s.getUtility($1T.class)".formatted(fieldName, worldVar), type);
        }

        @Override
        public void systemAccessor(Builder code) {
            code.add(fieldName);
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            code.add(fieldName);
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
            writer.append(type.toString()).append(System.lineSeparator());
        }

        public static UtilityParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();
            var type = ParsedType.parse(reader.readLine()).getClassName();

            return new UtilityParameter(null, name, type);
        }

    }

    record SingletonParameter(VariableElement parameter, String name, ClassName type,
                              String fieldName) implements SystemParameterData, FieldProvider {

        private static final String TYPE = "SINGLETON";

        public SingletonParameter(VariableElement parameter, String name, ClassName type) {
            this(parameter, name, type, Utils.decapitalize(type.simpleName()));
        }

        @Override
        public FieldSpec fieldSpec() {
            return FieldSpec.builder(type, fieldName, Modifier.PRIVATE, Modifier.FINAL).build();
        }

        @Override
        public void fieldInit(Builder code, String worldVar, String archetypeDataVar) {
            code.addStatement("this.%s = %s.getSingleton($1T.class)".formatted(fieldName, worldVar), type);
        }

        @Override
        public void systemAccessor(Builder code) {
            code.add(fieldName);
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            code.add(fieldName);
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
            writer.append(type.toString()).append(System.lineSeparator());
        }

        public static SingletonParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();
            var type = ParsedType.parse(reader.readLine()).getClassName();

            return new SingletonParameter(null, name, type);
        }

    }

    sealed interface EntityParameterData extends ParameterData {
    }

    record EntityIdParameter(VariableElement parameter, String name) implements EntityParameterData {

        private static final String TYPE = "ENTITY_ID";

        @Override
        public TypeName type() {
            return TypeName.INT;
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            code.add("%s[%s]".formatted(entitiesArrayVar, indexVar));
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
        }

        public static EntityIdParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();

            return new EntityIdParameter(null, name);
        }

    }

    record EntityParameter(VariableElement parameter, String name) implements EntityParameterData {

        private static final String TYPE = "ENTITY";

        @Override
        public TypeName type() {
            return Utils.ENTITY_REF;
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            code.add("_entity");
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
        }

        public static EntityParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();

            return new EntityParameter(null, name);
        }

    }

    record ComponentParameter(VariableElement parameter, String name, ComponentData data,
                              String fieldName) implements EntityParameterData, LocalVariableProvider, LoopInitProvider, CleanUpProvider {

        private static final String TYPE = "COMPONENT";

        public ComponentParameter(VariableElement parameter, ComponentData data) {
            this(parameter, parameter.getSimpleName().toString(), data, data.fieldName());
        }

        public Stream<FieldSpec> fieldSpec(CompositionData composition, boolean inline, AbstractGenerator generator) {
            return switch (data) {
                case ComponentData.ClassComponent component ->
                        Stream.of(FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, type()), fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                case ComponentData.EnumComponent component ->
                        Stream.of(FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, type()), fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                case ComponentData.SingletonEnumComponent component -> {
                    var all = composition.all() != null && composition.all().contains(type());
                    var none = composition.none() != null && composition.none().contains(type());

                    if (!all && !none) {
                        yield Stream.of(FieldSpec.builder(component.className(), fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                    }

                    yield Stream.empty();
                }
                case ComponentData.InterfaceComponent component when inline -> Stream.concat(
                        Stream.of(FieldSpec.builder(TypeName.BOOLEAN, fieldName + "Present", Modifier.PRIVATE, Modifier.FINAL).build()),
                        component.fields().stream()
                                .map(field -> EntityArchetypeDataGenerator.createInterfaceComponentField(component, field, generator).field()));
                case ComponentData.InterfaceComponent component -> Stream.of(
                        FieldSpec.builder(component.impl(), fieldName, Modifier.PRIVATE, Modifier.FINAL).build(),
                        FieldSpec.builder(TypeName.BOOLEAN, fieldName + "Present", Modifier.PRIVATE, Modifier.FINAL).build());
            };
        }

        public void fieldInit(Builder code, String worldVar, String archetypeVar, String archetypeDataVar, CompositionData composition, boolean inline, AbstractGenerator generator) {
            switch (data) {
                case ComponentData.ClassComponent component ->
                        code.addStatement("this.%1$s = %2$s.%1$s".formatted(fieldName, archetypeDataVar));
                case ComponentData.EnumComponent component ->
                        code.addStatement("this.%1$s = %2$s.%1$s".formatted(fieldName, archetypeDataVar));
                case ComponentData.SingletonEnumComponent component -> {
                    var all = composition.all() != null && composition.all().contains(type());
                    var none = composition.none() != null && composition.none().contains(type());

                    if (!all && !none) {
                        code.addStatement("this.%s = %s.getComponents().contains($1T.class) ? $1T.%s : null".formatted(fieldName, archetypeVar, component.instance()), component.className());
                    }
                }
                case ComponentData.InterfaceComponent component when inline -> {
                    for (var field : component.fields()) {
                        var fieldSpec = EntityArchetypeDataGenerator.createInterfaceComponentField(component, field, generator);
                        code.addStatement("this.%1$s = %2$s.%1$s".formatted(fieldSpec.field().name(), archetypeDataVar));
                    }

                    code.addStatement("this.%sPresent = archetype.getComponents().contains($1T.class)".formatted(fieldName), component.className());
                }
                case ComponentData.InterfaceComponent component -> {
                    code.addStatement("this.%s = new $1T(archetype.getData())".formatted(fieldName), component.impl());
                    code.addStatement("this.%sPresent = archetype.getComponents().contains($1T.class)".formatted(fieldName), component.className());
                }
            }
        }

        @Override
        public void localVariable(Builder code, CompositionData composition) {
            if (composition.all() != null && composition.all().contains(type())) {
                switch (data) {
                    case ComponentData.ClassComponent component ->
                            code.addStatement("$1T[] %s = this.%s.getData()".formatted(name, fieldName), type());
                    case ComponentData.EnumComponent component ->
                            code.addStatement("$1T[] %s = this.%s.getData()".formatted(name, fieldName), type());
                    case ComponentData.SingletonEnumComponent component -> {
                    }
                    case ComponentData.InterfaceComponent component -> {
                    }
                }
            } else if (composition.none() != null && composition.none().contains(type())) {
                // no local variables (condition not inverted to keep same structure as #entityAccessor
            } else {
                switch (data) {
                    case ComponentData.ClassComponent component ->
                            code.addStatement("$1T[] %1$s = this.%2$s != null ? this.%2$s.getData() : null".formatted(name, fieldName), type());
                    case ComponentData.EnumComponent component ->
                            code.addStatement("$1T[] %1$s = this.%2$s != null ? this.%2$s.getData() : null".formatted(name, fieldName), type());
                    case ComponentData.SingletonEnumComponent component -> {
                    }
                    case ComponentData.InterfaceComponent component -> {
                        code.addStatement("$1T %1$s = this.%1$sPresent ? this.%1$s : null".formatted(fieldName), component.impl());
                    }
                }
            }
        }

        @Override
        public void loopInit(Builder code, String indexVar, boolean inline) {
            if (data instanceof InterfaceComponent component && !inline) {
                code.addStatement("this.%s.index = %s".formatted(fieldName, indexVar));
            }
        }

        @Override
        public void cleanUp(Builder code, boolean inline) {
            if (data instanceof InterfaceComponent component && !inline) {
                code.addStatement("this.%s.index = -1".formatted(fieldName));
            }
        }

        @Override
        public ClassName type() {
            return data.className();
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            if (composition.all() != null && composition.all().contains(type())) {
                switch (data) {
                    case ComponentData.ClassComponent component -> code.add("%s[%s]".formatted(name, indexVar));
                    case ComponentData.EnumComponent component -> code.add("%s[%s]".formatted(name, indexVar));
                    case ComponentData.SingletonEnumComponent component ->
                            code.add("$1T.%s".formatted(component.instance()), component.className());
                    case ComponentData.InterfaceComponent component -> {
                        code.add("this.%s".formatted(fieldName));
                    }
                }
            } else if (composition.none() != null && composition.none().contains(type())) {
                code.add("null");
            } else {
                switch (data) {
                    case ComponentData.ClassComponent component ->
                            code.add("%1$s != null ? %1$s[%2$s] : null".formatted(name, indexVar));
                    case ComponentData.EnumComponent component ->
                            code.add("%1$s != null ? %1$s[%2$s] : null".formatted(name, indexVar));
                    case ComponentData.SingletonEnumComponent component -> code.add("this.%s".formatted(fieldName));
                    case ComponentData.InterfaceComponent component -> {
                        code.add("%s".formatted(fieldName));
                    }
                }
            }
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
            data.writeMetadata(writer);
            writer.append(fieldName).append(System.lineSeparator());
        }

        public static ComponentParameter readMetadata(BufferedReader reader) throws IOException {
            var name = reader.readLine();
            var data = ComponentData.readMetadata(reader);
            var fieldName = reader.readLine();

            return new ComponentParameter(null, name, data, fieldName);
        }

    }

    record InvalidParameter(VariableElement parameter, String name) implements ParameterData {

        @Override
        public TypeName type() {
            throw new UnsupportedOperationException("InvalidParameter should not be used!");
        }

        @Override
        public void entityAccessor(Builder code, String entitiesArrayVar, String indexVar, CompositionData composition) {
            throw new UnsupportedOperationException("InvalidParameter should not be used!");
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            throw new UnsupportedOperationException("writeManifest not supported: " + this);
        }

    }

}
