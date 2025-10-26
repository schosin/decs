package de.schosin.decs.codegen.invocation;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.system.SystemsResult;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.ParsedType;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SystemInvocationGenerator extends AbstractGenerator {

    private final SystemsResult systems;

    private final Detector detector = new Detector();
    private final Generator generator = new Generator();

    private final Map<TypeElement, List<SystemGroup>> types = new HashMap<>();
    private final Map<ClassName, ClassName> implementations = new HashMap<>();

    public SystemInvocationGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components, SystemsResult systems) {
        super(processingEnv, roundEnv, components);

        this.systems = systems;
    }

    public void process() {
        roundEnv.getElementsAnnotatedWith(annotations.systems()).stream()
                .map(TypeElement.class::cast)
                .forEach(this.detector::process);

        if (isError()) {
            return;
        }

        validateSystems();
    }

    public List<JavaType> generate() {
        return this.generator.generate();
    }

    public Map<ClassName, ClassName> getImplementations() {
        return implementations;
    }

    private void validateSystems() {
        for (var entry : this.types.entrySet()) {
            var def = entry.getKey();
            var groups = entry.getValue();

            validateSystems(def, groups);
        }
    }

    private void validateSystems(TypeElement def, List<SystemGroup> groups) {
        var seenTypes = new HashSet<ClassName>();
        var seenGroupNames = new HashSet<String>();

        for (var group : groups) {
            switch (group.strategy) {
                case SEQUENTIAL -> validateSequentialSystemGroup(def, group);
                case PARALLEL -> validateParallelSystemGroup(def, group);
            }

            if (!"".equals(group.name) && !seenGroupNames.add(group.name.toLowerCase())) {
                printError("%d. @SystemGroup reuses group name '%s' (case insensitive).".formatted(group.index + 1, group.name), def);
            }

            for (var system : group.systems) {
                if (!seenTypes.add(system)) {
                    printError("Type '%s' is used more than once.".formatted(system), def);
                }
            }
        }
    }

    private void validateSequentialSystemGroup(TypeElement def, SystemGroup group) {
        // nothing yet
    }

    private void validateParallelSystemGroup(TypeElement def, SystemGroup group) {
        if (group.systems.size() < 2) {
            printError("%d. @SystemGroup uses parallel strategy, but defines only one system. Use sequential strategy or add more systems.".formatted(group.index + 1), def);
        }
    }

    private class Detector {

        @SuppressWarnings("unchecked")
        public void process(TypeElement def) {
            if (!validate(def)) {
                return;
            }

            var systemsAnnotation = resolveAnnotation(def, Utils.SYSTEMS);
            var systemGroups = systemsAnnotation.getElementValues().entrySet().stream()
                    .filter(entry -> entry.getKey().getSimpleName().contentEquals("value"))
                    .findFirst()
                    .map(entry -> (List<AnnotationMirror>) entry.getValue().getValue())
                    .orElseThrow(() -> new IllegalStateException("Expected Systems#value to exist"));

            if (systemGroups.isEmpty()) {
                printError("@Systems contains no systems.", def);
                return;
            }

            var systemsList = new ArrayList<SystemGroup>();
            for (int i = 0, s = systemGroups.size(); i < s; i++) {
                systemsList.add(processSystems(systemGroups.get(i), def, i));
            }

            types.put(def, systemsList);

            var className = ClassName.get(def);
            implementations.put(className, Generator.getImplementation(className));
        }

        @SuppressWarnings("unchecked")
        private SystemGroup processSystems(AnnotationMirror annotation, TypeElement def, int index) {
            var name = "";
            var enabled = true;
            var strategy = InvocationStrategy.SEQUENTIAL;
            var systemTypes = List.<TypeMirror>of();

            for (var entry : annotation.getElementValues().entrySet()) {
                var value = entry.getValue().getValue();

                switch (entry.getKey().getSimpleName().toString()) {
                    case "name" -> name = (String) value;
                    case "enabled" -> enabled = Boolean.TRUE.equals(value);
                    case "strategy" -> strategy = InvocationStrategy.valueOf(value.toString());
                    case "value" -> systemTypes = ((List<AnnotationValue>) value).stream()
                            .map(AnnotationValue::getValue)
                            .map(TypeMirror.class::cast)
                            .toList();
                    default -> {
                        throw new IllegalStateException("Unhandled @Systems attribute '%s': %s".formatted(entry.getKey().getSimpleName(), value));
                    }
                }
            }

            if (!"".equals(name) && !SourceVersion.isIdentifier(name)) {
                printError("Name '%s' of %d. @SystemGroup is not a valid identifier.".formatted(name, index), def);
                return null;
            }

            if (name.endsWith("Enabled")) {
                printError("Name '%s' of %d. @SystemGroup must not end with 'Enabled'.".formatted(name, index), def);
                return null;
            }

            var systems = new ArrayList<ClassName>(systemTypes.size());
            for (var systemType : systemTypes) {
                if (!(systemType instanceof DeclaredType declared)) {
                    printError("Type '%s' in %d. @SystemGroup is not a valid system type".formatted(systemType, index + 1), def);
                    continue;
                }

                var element = (TypeElement) declared.asElement();
                if (SystemInvocationGenerator.this.systems.getSystem(element) == null) {
                    printError("Type '%s' in %d. @SystemGroup is not a system type. Only types containing system annotations can be used.".formatted(systemType, index + 1), def);
                    continue;
                }

                systems.add(ClassName.get(element));
            }

            if (isError()) {
                return null;
            }

            if (systems.isEmpty()) {
                printError("%d. @SystemGroup declares no systems".formatted(index), def);
                return null;
            }

            var groupName = "".equals(name) ? "Unnamed group (index %d)".formatted(index) : name;
            return new SystemGroup(def, index, name, groupName, enabled, strategy, systems);
        }

        private boolean validate(TypeElement def) {
            if (def.getKind() != ElementKind.INTERFACE) {
                printError("@Systems must be used on an interface extending SystemInvocation.", def);
                return false;
            }

            if (def.getInterfaces().size() != 1 || !Utils.SYSTEM_INVOCATION.canonicalName().equals(def.getInterfaces().getFirst().toString())) {
                printError("@Systems must be used on an interface extending SystemInvocation.", def);
                return false;
            }

            if (!def.getModifiers().contains(Modifier.PUBLIC)) {
                printError("Interfaces annotated with @Systems must be public top-level types. Make it public.", def);
                return false;
            }

            var parent = def.getEnclosingElement();
            while (parent instanceof TypeElement parentElement) {
                if (!parentElement.getModifiers().contains(Modifier.PUBLIC)) {
                    printError("Interfaces annotated with @Systems must be accessible. Interface is nested in a non-public parent type.", def);
                    return false;
                }

                parent = parent.getEnclosingElement();
            }

            if (parent == null || parent.getKind() != ElementKind.PACKAGE) {
                printError("Interfaces annotated with @Systems must not be in the default package. Move it to a named package.", def);
                return false;

            }

            if (!def.getTypeParameters().isEmpty()) {
                printError("Interfaces annotated with @Systems must not be generic. Remove the type parameters.", def);
                return false;
            }

            if (def.getModifiers().contains(Modifier.SEALED)) {
                printError("Interfaces annotated with @Systems must not be sealed. Remove the 'sealed' keyword.", def);
                return false;
            }

            var abstractMethods = def.getEnclosedElements().stream()
                    .filter(ExecutableElement.class::isInstance)
                    .map(ExecutableElement.class::cast)
                    .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                    .toList();

            if (!abstractMethods.isEmpty()) {
                for (var method : abstractMethods) {
                    printError("Interfaces annotated with @Systems must not contain abstract methods. Remove this method.", method);
                }

                return false;
            }

            return true;
        }

    }

    private class Generator {

        private static ClassName getImplementation(ClassName def) {
            return ClassName.get(def.packageName(), def.simpleName() + "Impl");
        }

        public List<JavaType> generate() {
            var types = Manifest.getTypes(SystemInvocationGenerator.this);

            return types.entrySet().stream()
                    .map(entry -> generate(entry.getKey(), entry.getValue()))
                    .toList();
        }

        private JavaType generate(ClassName def, List<SystemGroup> groups) {
            var className = getImplementation(def);

            var hasParallelGroup = groups.stream().anyMatch(group -> group.strategy == InvocationStrategy.PARALLEL);

            var type = TypeSpec.classBuilder(className)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(def)
                    .addField(Utils.INTERNAL_WORLD, "_world", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(executor(hasParallelGroup))
                    .addFields(systemFields(groups))
                    .addFields(systemGroupFields(groups))
                    .addMethod(constructor(def, groups, hasParallelGroup))
                    .addMethod(runSystems(groups))
                    .addMethod(toggleSystemGroup(groups, "enableSystemGroup", true))
                    .addMethod(toggleSystemGroup(groups, "disableSystemGroup", false))
                    .addMethod(getSystems(groups))
                    .build();

            return JavaType.create(className.packageName(), type);
        }

        private List<FieldSpec> executor(boolean hasParallelGroup) {
            if (!hasParallelGroup) {
                return List.of();
            }

            return List.of(FieldSpec.builder(ExecutorService.class, "_executor", Modifier.PRIVATE, Modifier.FINAL).build());
        }

        private List<FieldSpec> systemFields(List<SystemGroup> groups) {
            return groups.stream()
                    .flatMap(this::createSystemFields)
                    .toList();
        }

        private Stream<FieldSpec> createSystemFields(SystemGroup group) {
            return group.systems.stream()
                    .map(this::createSystemField);
        }

        private FieldSpec createSystemField(ClassName system) {
            var data = SystemInvocationGenerator.this.systems.getSystem(system);
            if (data == null) {
                printError("%s: %s".formatted(system, SystemInvocationGenerator.this.systems.types()));
                return null;
            }
            var fieldName = Utils.decapitalize(system.simpleName());

            return FieldSpec.builder(data.impl(), fieldName, Modifier.PRIVATE, Modifier.FINAL).build();
        }

        private List<FieldSpec> systemGroupFields(List<SystemGroup> groups) {
            return groups.stream()
                    .filter(group -> !"".equals(group.name))
                    .map(group -> FieldSpec.builder(TypeName.BOOLEAN, Utils.decapitalize(group.name) + "Enabled", Modifier.PRIVATE)
                            .initializer(group.enabled ? "true" : "false")
                            .build())
                    .toList();
        }

        private MethodSpec constructor(ClassName def, List<SystemGroup> groups, boolean hasParallelGroup) {
            var code = CodeBlock.builder();
            code.addStatement("this._world = world");

            if (hasParallelGroup) {
                // TODO make it configurable
                code.addStatement("this._executor = $1T.newDefaultExecutor($2T.class.getSimpleName())", Utils.DECS_EXECUTORS, def);
            }

            for (var group : groups) {
                code.add(System.lineSeparator());
                code.add("// %s".formatted(group.groupName)).add(System.lineSeparator());

                for (var system : group.systems) {
                    var data = SystemInvocationGenerator.this.systems.getSystem(system);
                    var fieldName = Utils.decapitalize(system.simpleName());

                    code.addStatement("this.%s = ($1T) $2T.getSystemMetadata($3T.class).constructor().apply(world)".formatted(fieldName), data.impl(), Utils.TYPES, system);
                }
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Utils.INTERNAL_WORLD, "world")
                    .addCode(code.build())
                    .build();
        }

        private MethodSpec runSystems(List<SystemGroup> groups) {
            var code = CodeBlock.builder();

            // TODO eliminate branches (mutable Runnable field with methods for all possible combinations of disabled systems?)

            for (int i = 0, s = groups.size(); i < s; i++) {
                if (i > 0) {
                    code.add(System.lineSeparator());
                }

                var group = groups.get(i);
                code.add("// %s".formatted(group.groupName)).add(System.lineSeparator());

                var named = !"".equals(group.name);
                if (named) {
                    code.beginControlFlow("if (this.%sEnabled)".formatted(Utils.decapitalize(group.name)));
                }

                code.add(switch (group.strategy) {
                    case SEQUENTIAL -> runSequentialGroup(group);
                    case PARALLEL -> runParallelGroup(group);
                });

                if (named) {
                    code.endControlFlow();
                }
            }

            return MethodSpec.methodBuilder("runSystems")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addCode(code.build())
                    .build();
        }

        private CodeBlock runSequentialGroup(SystemGroup group) {
            var code = CodeBlock.builder();

            for (var system : group.systems) {
                var fieldName = Utils.decapitalize(system.simpleName());
                code.addStatement("this.%s.runSystem()".formatted(fieldName));
            }

            return code.build();
        }

        private CodeBlock runParallelGroup(SystemGroup group) {
            var code = CodeBlock.builder();

            for (var system : group.systems) {
                var fieldName = Utils.decapitalize(system.simpleName());
                code.addStatement("$1T<$2T> %1$sFuture = this._executor.submit(this.%1$s)".formatted(fieldName), Future.class, Void.class);
            }
            code.beginControlFlow("try");
            for (var system : group.systems) {
                var fieldName = Utils.decapitalize(system.simpleName());
                code.addStatement("%sFuture.get()".formatted(fieldName));
            }
            code.nextControlFlow("catch ($1T | $2T ex)", InterruptedException.class, ExecutionException.class);
            code.addStatement("throw new $1T(\"Exception while executing parallel system group '%s': \" + ex.getMessage(), ex)".formatted(group.groupName), Utils.SYSTEM_INVOCATION_EXCEPTION);
            code.endControlFlow();

            return code.build();
        }

        private MethodSpec toggleSystemGroup(List<SystemGroup> groups, String methodName, boolean value) {
            var code = CodeBlock.builder();

            code.beginControlFlow("switch(name)");
            for (var group : groups) {
                if ("".equals(group.name)) {
                    continue;
                }

                code.add("case \"%s\":".formatted(group.name)).add(System.lineSeparator())
                        .indent()
                        .addStatement("this.%sEnabled = %b".formatted(Utils.decapitalize(group.name), value))
                        .addStatement("break")
                        .unindent();
            }

            code.add("default:").add(System.lineSeparator())
                    .indent()
                    .addStatement("throw new $1T(\"Unknown system group: \" + name)", IllegalArgumentException.class)
                    .unindent();
            code.endControlFlow();

            return MethodSpec.methodBuilder(methodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(String.class, "name")
                    .addCode(code.build())
                    .build();
        }

        private MethodSpec getSystems(List<SystemGroup> groups) {
            var code = CodeBlock.builder();

            code.add("return new $1T[] {", Utils.SYSTEM_TYPE);
            code.add(System.lineSeparator()).indent();

            var count = 0;
            for (int i = 0, s = groups.size(); i < s; i++) {
                if (i > 0) {
                    code.add(System.lineSeparator());
                }

                var group = groups.get(i);
                code.add("// %s".formatted(group.groupName)).add(System.lineSeparator());

                for (int j = 0, js = group.systems.size(); j < js; j++) {
                    var system = group.systems.get(j);
                    var fieldName = Utils.decapitalize(system.simpleName());

                    code.add("this.%s".formatted(fieldName));

                    if (i != s - 1 || j != js - 1) {
                        code.add(", ");
                    }
                }
            }

            code.unindent().add(System.lineSeparator());
            code.addStatement("}");

            return MethodSpec.methodBuilder("getSystems")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(ArrayTypeName.of(Utils.SYSTEM_TYPE))
                    .addCode(code.build())
                    .build();
        }

    }

    private static class Manifest {

        private static final String DIR = "META-INF/decs/invocations/";
        private static final String MANIFEST = DIR + "manifest";

        public static Map<ClassName, List<SystemGroup>> getTypes(SystemInvocationGenerator generator) {
            var currentTypes = generator.types.keySet().stream().map(TypeElement::toString).collect(Collectors.toSet());

            var result = new TreeMap<ClassName, List<SystemGroup>>();
            result.putAll(readManifest(generator, currentTypes));
            result.putAll(convert(generator.types));

            generator.implementations.clear();
            for (var className : result.keySet()) {
                generator.implementations.put(className, Generator.getImplementation(className));
            }

            writeManifest(result, generator);

            return result;
        }

        private static Map<ClassName, List<SystemGroup>> readManifest(SystemInvocationGenerator generator, Set<String> currentTypes) {
            var filer = generator.processingEnv.getFiler();
            var result = new TreeMap<ClassName, List<SystemGroup>>();

            try {
                var file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", MANIFEST);

                try (var manifest = new BufferedReader(new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = manifest.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }

                        var metadata = filer.getResource(StandardLocation.CLASS_OUTPUT, "", DIR + line);
                        try (var reader = new LineNumberReader(new InputStreamReader(metadata.openInputStream(), StandardCharsets.UTF_8))) {
                            var entry = readMetadata(reader);
                            result.put(entry.getKey(), entry.getValue());
                        } catch (NoSuchFileException ex) {
                            if (!currentTypes.contains(line)) {
                                generator.printError("Failed to read invocation metadata for '%s': No such file (NoSuchFileException)".formatted(line));
                                return result;
                            }
                        } catch (IOException ex) {
                            // Eclipse JDT does not throw NoSuchFileException
                            if (ex.getMessage().contains("does not exist")) {
                                if (!currentTypes.contains(line)) {
                                    generator.printError("Failed to read invocation metadata for '%s': No such file (IOException)".formatted(line));
                                }

                                continue;
                            }

                            var writer = new StringWriter();
                            ex.printStackTrace(new PrintWriter(writer));

                            generator.printError("Failed to read invocation metadata for '%s': %s%s%s".formatted(line, ex.getMessage(), System.lineSeparator(), writer.toString()));
                            return result;
                        }
                    }
                }

                return result;
            } catch (NoSuchFileException ex) {
                return result;
            } catch (IOException ex) {
                // Eclipse JDT does not throw NoSuchFileException
                if (ex.getMessage().contains("does not exist")) {
                    return result;
                }

                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read invocations manifest: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
                return result;
            }
        }

        private static void writeManifest(Map<ClassName, List<SystemGroup>> result, SystemInvocationGenerator generator) {
            var filer = generator.processingEnv.getFiler();
            var originatingElements = result.values().stream()
                    .flatMap(List::stream)
                    .map(SystemGroup::element)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toArray(Element[]::new);

            try {
                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", MANIFEST, originatingElements);

                try (var writer = file.openWriter()) {
                    for (var entry : result.entrySet()) {
                        writer.append(entry.getKey().canonicalName()).append(System.lineSeparator());
                        writeMetadata(entry, generator);
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write invocation manifest: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
            }
        }

        private static Map.Entry<ClassName, List<SystemGroup>> readMetadata(BufferedReader reader) throws IOException {
            var className = ParsedType.parse(reader.readLine()).getClassName();

            var count = Integer.parseInt(reader.readLine());
            var groups = new ArrayList<SystemGroup>(count);
            for (int i = 0; i < count; i++) {
                groups.add(SystemGroup.readMetadata(reader));
            }

            return Map.entry(className, groups);
        }

        private static void writeMetadata(Entry<ClassName, List<SystemGroup>> metadata, SystemInvocationGenerator generator) throws IOException {
            var className = metadata.getKey();
            var groups = metadata.getValue();

            var fqcn = className.canonicalName();
            var filer = generator.processingEnv.getFiler();

            var element = groups.getFirst().element;

            try {
                var resourcePath = DIR + fqcn;
                var originatingElements = element != null
                        ? new Element[]{element}
                        : new Element[0];

                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath, originatingElements);
                try (var writer = file.openWriter()) {
                    writer.append(className.toString()).append(System.lineSeparator());

                    writer.append(String.valueOf(groups.size())).append(System.lineSeparator());
                    for (var group : groups) {
                        group.writeMetadata(writer);
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write invocation metadata for '%s': %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()), element);
            }
        }

        private static Map<ClassName, List<SystemGroup>> convert(Map<TypeElement, List<SystemGroup>> types) {
            return types.entrySet().stream()
                    .map(entry -> Map.entry(ClassName.get(entry.getKey()), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    }

    private enum InvocationStrategy {
        SEQUENTIAL, PARALLEL
    }

    private record SystemGroup(TypeElement element, int index, String name, String groupName, boolean enabled,
                               InvocationStrategy strategy, List<ClassName> systems) {

        void writeMetadata(Writer writer) throws IOException {
            writer.append(Integer.toString(index)).append(System.lineSeparator());
            writer.append(name).append(System.lineSeparator());
            writer.append(groupName).append(System.lineSeparator());
            writer.append(enabled ? "true" : "false").append(System.lineSeparator());
            writer.append(strategy.name()).append(System.lineSeparator());

            writer.append(Integer.toString(systems.size())).append(System.lineSeparator());
            for (var system : systems) {
                writer.append(system.toString()).append(System.lineSeparator());
            }
        }

        static SystemGroup readMetadata(BufferedReader reader) throws IOException {
            var index = Integer.parseInt(reader.readLine());
            var name = reader.readLine();
            var groupName = reader.readLine();
            var enabled = "true".equals(reader.readLine());
            var strategy = InvocationStrategy.valueOf(reader.readLine());

            var count = Integer.parseInt(reader.readLine());
            var systems = new ArrayList<ClassName>(count);
            for (int i = 0; i < count; i++) {
                systems.add(ParsedType.parse(reader.readLine()).getClassName());
            }

            return new SystemGroup(null, index, name, groupName, enabled, strategy, systems);
        }

    }


}
