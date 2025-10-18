package de.schosin.decs.codegen.system;

import com.palantir.javapoet.ClassName;
import de.schosin.decs.codegen.system.methods.CallbackMethod;
import de.schosin.decs.codegen.system.methods.EcsMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod;
import de.schosin.decs.codegen.utils.ManifestUtils;
import de.schosin.decs.codegen.utils.ParsedType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public sealed interface TypeData {

    TypeElement element();

    ClassName className();

    List<UtilityMethod> utils();

    void writeMetadata(Writer writer) throws IOException;

    static TypeData readMetadata(BufferedReader reader) throws IOException {
        var type = reader.readLine();
        return switch (type) {
            case SystemData.TYPE -> SystemData.readMetadata(reader);
            case UtilityData.TYPE -> UtilityData.readMetadata(reader);
            default -> throw new IllegalArgumentException("Unknown type '%s'. Perform a clean build.".formatted(type));
        };
    }

    record SystemData(TypeElement element, ClassName className, CompositionData composition, List<SystemMethod> methods,
            List<CallbackMethod> callbacks, List<UtilityMethod> utils) implements TypeData {

        private static final String TYPE = "SYSTEM";

        public SystemData(TypeElement element, CompositionData composition, SystemMethod method) {
            this(element, ClassName.get(element), composition, new ArrayList<>(List.of(method)), new ArrayList<>(), new ArrayList<>());
        }

        public SystemData(TypeElement element, CompositionData composition, CallbackMethod method) {
            this(element, ClassName.get(element), composition, new ArrayList<>(), new ArrayList<>(List.of(method)), new ArrayList<>());
        }

        public SystemData(TypeElement element, CompositionData composition, UtilityMethod method) {
            this(element, ClassName.get(element), composition, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(List.of(method)));
        }

        public boolean modifying() {
            // Only utils so far. If constructor DI is implemented, might need to check for fields
            return !utils.isEmpty();
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(className.toString()).append(System.lineSeparator());
            ManifestUtils.writeCompositionData(composition, writer);

            writer.append(String.valueOf(methods.size())).append(System.lineSeparator());
            for (var method : methods) {
                method.writeMetadata(writer);
            }

            writer.append(String.valueOf(callbacks.size())).append(System.lineSeparator());
            for (var method : callbacks) {
                method.writeMetadata(writer);
            }

            writer.append(String.valueOf(utils.size())).append(System.lineSeparator());
            for (var method : utils) {
                method.writeMetadata(writer);
            }
        }

        static SystemData readMetadata(BufferedReader reader) throws IOException {
            var className = ParsedType.parse(reader.readLine()).getClassName();
            var compositionData = ManifestUtils.readCompositionData(reader, "System '%s'".formatted(className));

            var methodCount = Integer.parseInt(reader.readLine());
            var methods = new ArrayList<SystemMethod>(methodCount);
            for (int i = 0; i < methodCount; i++) {
                methods.add(SystemMethod.readMetadata(reader));
            }

            var callbackCount = Integer.parseInt(reader.readLine());
            var callbacks = new ArrayList<CallbackMethod>(callbackCount);
            for (int i = 0; i < callbackCount; i++) {
                callbacks.add(CallbackMethod.readMetadata(reader));
            }

            var utilCount = Integer.parseInt(reader.readLine());
            var utils = new ArrayList<UtilityMethod>(utilCount);
            for (int i = 0; i < utilCount; i++) {
                utils.add(UtilityMethod.readMetadata(reader));
            }

            return new SystemData(null, className, compositionData, methods, callbacks, utils);
        }

    }

    record UtilityData(TypeElement element, ClassName className, List<UtilityMethod> utils) implements TypeData {

        private static final String TYPE = "UTILITY";

        public UtilityData(TypeElement element, UtilityMethod method) {
            this(element, ClassName.get(element), new ArrayList<>(List.of(method)));
        }

        @Override
        public void writeMetadata(Writer writer) throws IOException {
            writer.append(TYPE).append(System.lineSeparator());
            writer.append(className.toString()).append(System.lineSeparator());

            writer.append(String.valueOf(utils.size())).append(System.lineSeparator());
            for (var method : utils) {
                method.writeMetadata(writer);
            }
        }

        static UtilityData readMetadata(BufferedReader reader) throws IOException {
            var className = ParsedType.parse(reader.readLine()).getClassName();

            var count = Integer.parseInt(reader.readLine());
            var utils = new ArrayList<UtilityMethod>(count);
            for (int i = 0; i < count; i++) {
                var method = UtilityMethod.readMetadata(reader);
                utils.add(method);
            }

            return new UtilityData(null, className, utils);
        }

    }

    static TypeData merge(TypeData first, TypeData second) {
        if (first.element() == null) {
            return second;
        }
        if (second.element() == null) {
            return first;
        }

        if (first instanceof UtilityData && second instanceof UtilityData) {
            first.utils().addAll(second.utils());
            return first;
        }

        if (first instanceof SystemData firstSystem) {
            first.utils().addAll(second.utils());

            if (second instanceof SystemData secondSystem) {
                firstSystem.methods.addAll(secondSystem.methods);
                firstSystem.callbacks.addAll(secondSystem.callbacks);

                // sort methods and callbacks to preserve same order as in source file
                firstSystem.methods.sort(systemMethodComparator(firstSystem.element));
                firstSystem.callbacks.sort(systemMethodComparator(firstSystem.element));
            }

            return first;
        }

        second.utils().addAll(first.utils());
        return second;
    }

    private static Comparator<? super EcsMethod> systemMethodComparator(TypeElement element) {
        var elements = element.getEnclosedElements();

        var lookup = new HashMap<ExecutableElement, Integer>();
        for (int i = 0, s = element.getEnclosedElements().size(); i < s; i++) {
            if (elements.get(i) instanceof ExecutableElement method) {
                lookup.put(method, i);
            }
        }

        return (first, second) -> lookup.get(first.method()) - lookup.get(second.method());
    }

}