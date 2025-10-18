package de.schosin.decs.codegen.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import com.palantir.javapoet.ClassName;

import de.schosin.decs.codegen.utils.ParsedType;
import de.schosin.decs.codegen.utils.Utils;

/**
 * Models the supported component types.
 */
public sealed interface ComponentData {

    DeclaredType type();

    TypeElement element();

    ClassName className();

    default String fieldName() {
        return Utils.decapitalize(className().simpleName()) + "Data";
    }
    default String poolFieldName() {
        return Utils.decapitalize(className().simpleName()) + "Pool";
    }

    void writeMetadata(Writer writer) throws IOException;

    static ComponentData readMetadata(BufferedReader reader) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case ClassComponent.TYPE -> ClassComponent.readMetadata(reader);
            case EnumComponent.TYPE -> EnumComponent.readMetadata(reader);
            case SingletonEnumComponent.TYPE -> SingletonEnumComponent.readMetadata(reader);
            default -> throw new IllegalArgumentException("Unknown component type '%s'. Perform a clean build.".formatted(type));
        };
    }

    record ClassComponent(DeclaredType type, TypeElement element, ClassName className) implements ComponentData {

        private static final String TYPE = "CLASS";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(className.toString()).append(System.lineSeparator());
        }

        public static ClassComponent readMetadata(BufferedReader reader) throws IOException {
            var className = ParsedType.parse(reader.readLine()).getClassName();

            return new ClassComponent(null, null, className);
        }

    }

    sealed interface EnumComponentData extends ComponentData {
    }

    record EnumComponent(DeclaredType type, TypeElement element, ClassName className) implements EnumComponentData {

        private static final String TYPE = "ENUM";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(className.toString()).append(System.lineSeparator());
        }

        public static EnumComponent readMetadata(BufferedReader reader) throws IOException {
            var className = ParsedType.parse(reader.readLine()).getClassName();

            return new EnumComponent(null, null, className);
        }

    }

    record SingletonEnumComponent(DeclaredType type, TypeElement element, ClassName className, String instance) implements EnumComponentData {

        private static final String TYPE = "SINGLETON_ENUM";

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(className.toString()).append(System.lineSeparator());
            writer.append(instance).append(System.lineSeparator());
        }

        public static SingletonEnumComponent readMetadata(BufferedReader reader) throws IOException {
            var className = ParsedType.parse(reader.readLine()).getClassName();
            var instance = reader.readLine();

            return new SingletonEnumComponent(null, null, className, instance);
        }

    }

}
