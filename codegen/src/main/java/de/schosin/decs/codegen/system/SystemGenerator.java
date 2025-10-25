package de.schosin.decs.codegen.system;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.system.TypeData.SystemData;
import de.schosin.decs.codegen.system.TypeData.UtilityData;
import de.schosin.decs.codegen.system.helper.*;
import de.schosin.decs.codegen.system.helper.ProcessorGenerator.ProcessorResult;
import de.schosin.decs.codegen.system.methods.ProcessorMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod.EntityProcessorMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.ArchetypeMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.CountMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.TransmuteMethod;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.JavaType.StaticImport;
import de.schosin.decs.codegen.utils.ParameterData.FieldProvider;
import de.schosin.decs.codegen.utils.ParameterData.SingletonParameter;
import de.schosin.decs.codegen.utils.ParameterData.SystemParameterData;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SystemGenerator extends AbstractGenerator {

    private final SystemsResult systems;
    public final Set<TypeElement> seenTypes;
    public final Set<TypeElement> seenSystems;
    public final Set<TypeElement> seenUtilities;

    private final Detector detector = new Detector();

    private final ProcessorGenerator processor;
    private final InsertedGenerator inserted;
    private final RemovedGenerator removed;
    private final ArchetypeGenerator archetype;
    private final TransmuteGenerator transmute;
    private final CountGenerator count;

    private final SystemImplementation systemImpl;
    private final UtilityImplementation utilityImpl;
    private final TypesImplementation typesImpl;

    public SystemGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components, SystemsResult systems) {
        super(processingEnv, roundEnv, components);

        this.systems = systems;
        this.seenTypes = systems.seenTypes();
        this.seenSystems = systems.seenSystems();
        this.seenUtilities = systems.seenUtilities();

        this.processor = new ProcessorGenerator(processingEnv, roundEnv, this);
        this.inserted = new InsertedGenerator(processingEnv, roundEnv, this);
        this.removed = new RemovedGenerator(processingEnv, roundEnv, this);
        this.archetype = new ArchetypeGenerator(processingEnv, roundEnv, this);
        this.transmute = new TransmuteGenerator(processingEnv, roundEnv, this);
        this.count = new CountGenerator(processingEnv, roundEnv, this);

        this.systemImpl = new SystemImplementation();
        this.utilityImpl = new UtilityImplementation();
        this.typesImpl = new TypesImplementation();
    }

    public void process() {
        var types = detector.detectTypes();
        systems.types().addAll(types);
    }

    public List<JavaType> generate() {
        var types = Manifest.getTypes(systems.types(), SystemGenerator.this);
        if (isError() || types.isEmpty()) {
            return List.of();
        }

        var systems = types.stream()
                .filter(SystemData.class::isInstance)
                .map(SystemData.class::cast)
                .map(systemImpl::generate)
                .toList();

        if (!systems.isEmpty()) {
            printNote("Generating implementation for %d systems.".formatted(systems.size()));
        }

        var utilities = types.stream()
                .filter(UtilityData.class::isInstance)
                .map(UtilityData.class::cast)
                .map(utilityImpl::generate)
                .toList();

        if (!utilities.isEmpty()) {
            printNote("Generating implementation for %d utilities.".formatted(utilities.size()));
        }

        var result = new ArrayList<JavaType>();
        result.addAll(systems);
        result.addAll(utilities);
        result.add(SystemMetadata.create());
        result.add(typesImpl.generate(systems, utilities));

        return result;
    }

    private static class SystemMetadata {

        private static final String PACKAGE = "de.schosin.decs.values";
        private static final String NAME = "SystemMetadata";

        public static JavaType create() {
            // TODO move SystemMetadata to api module, remove generated type if possible

            var dependenciesType = ParameterizedTypeName.get(ClassName.get(Set.class), Utils.CLASS_WILDCARD);
            var constructorType = ParameterizedTypeName.get(ClassName.get(Function.class), ClassName.OBJECT, ClassName.OBJECT);

            var type = TypeSpec.classBuilder(NAME)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(Utils.CLASS_WILDCARD, "clazz", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.CLASS_WILDCARD, "implementation", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(constructorType, "constructor", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(dependenciesType, "dependencies", Modifier.PRIVATE, Modifier.FINAL)
                    .addMethod(constructor(constructorType, dependenciesType))
                    .addMethod(clazzAccessor())
                    .addMethod(implementationAccessor())
                    .addMethod(constructorAccessor(constructorType))
                    .addMethod(dependenciesAccessor(dependenciesType))
                    .build();

            return JavaType.create(PACKAGE, type);
        }

        private static MethodSpec constructor(ParameterizedTypeName constructorType, ParameterizedTypeName dependenciesType) {
            var code = CodeBlock.builder()
                    .addStatement("this.clazz = clazz")
                    .addStatement("this.implementation = implementation")
                    .addStatement("this.constructor = constructor")
                    .addStatement("this.dependencies = dependencies")
                    .build();

            return MethodSpec.constructorBuilder()
                    .addParameter(Utils.CLASS_WILDCARD, "clazz")
                    .addParameter(Utils.CLASS_WILDCARD, "implementation")
                    .addParameter(constructorType, "constructor")
                    .addParameter(dependenciesType, "dependencies")
                    .addCode(code)
                    .build();
        }

        private static MethodSpec clazzAccessor() {
            return MethodSpec.methodBuilder("clazz")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Utils.CLASS_WILDCARD)
                    .addStatement("return this.clazz")
                    .build();
        }

        private static MethodSpec implementationAccessor() {
            return MethodSpec.methodBuilder("implementation")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(Utils.CLASS_WILDCARD)
                    .addStatement("return this.implementation")
                    .build();
        }

        private static MethodSpec constructorAccessor(ParameterizedTypeName constructorType) {
            return MethodSpec.methodBuilder("constructor")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(constructorType)
                    .addStatement("return this.constructor")
                    .build();
        }

        private static MethodSpec dependenciesAccessor(ParameterizedTypeName dependenciesType) {
            return MethodSpec.methodBuilder("dependencies")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(dependenciesType)
                    .addStatement("return this.dependencies")
                    .build();
        }

    }

    private static class TypesImplementation {

        private static final String TYPES_PACKAGE = "de.schosin.decs.values";
        private static final String TYPES_NAME = "Types";

        public JavaType generate(List<SystemJavaType> systems, List<SystemJavaType> utilities) {
            var type = TypeSpec.classBuilder(TYPES_NAME)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(systemsField(systems))
                    .addStaticBlock(systemsInitializer(systems))
                    .addMethod(getSystemMetadata(systems))
                    .addMethod(getUtilities(utilities))
                    .addMethod(createArchetypeEntityData())
                    .addMethod(set())
                    .build();

            return JavaType.create(TYPES_PACKAGE, type);
        }


        private FieldSpec systemsField(List<SystemJavaType> systems) {
            var metadata = ClassName.get("", "SystemMetadata");
            var fieldType = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, metadata);

            return FieldSpec.builder(fieldType, "SYSTEMS", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build();
        }

        private CodeBlock systemsInitializer(List<SystemJavaType> systems) {
            var metadata = ClassName.get("", "SystemMetadata");
            var hashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, metadata);

            var code = CodeBlock.builder();
            code.addStatement("$1T systems = new $1T()", hashMap);

            for (var system : systems) {
                var dependencies = CodeBlock.builder();
                dependencies.add("set(");

                var idx = 0;
                for (var dependency : system.dependencies) {
                    if (idx++ > 0) {
                        dependencies.add(", ");
                    }

                    dependencies.add("$1T.class", dependency);
                }

                dependencies.add(")");

                code.add("systems.put($1T.class, new $2T($1T.class, $3T.class, world -> new $3T(($4T) world), ", system.source.className(), metadata, system.className(), Utils.INTERNAL_WORLD);
                code.add(dependencies.build());
                code.addStatement("))");
            }

            code.add(System.lineSeparator());
            code.addStatement("SYSTEMS = $1T.unmodifiableMap(systems)", Collections.class);

            return code.build();
        }

        private MethodSpec getSystemMetadata(List<SystemJavaType> systems) {
            var returnType = ClassName.get("", "SystemMetadata");

            return MethodSpec.methodBuilder("getSystemMetadata")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.CLASS_WILDCARD, "clazz")
                    .returns(returnType)
                    .addStatement("return SYSTEMS.get(clazz)")
                    .build();
        }

        private MethodSpec getUtilities(List<SystemJavaType> utilities) {
            // TODO eager utility instantiation might create unused EntityArchetypes for @Archetype methods

            var returnType = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, ClassName.OBJECT);
            var hashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, ClassName.OBJECT);

            var code = CodeBlock.builder();
            code.addStatement("$1T world = ($1T) arg", Utils.INTERNAL_WORLD);

            code.add(System.lineSeparator());
            code.addStatement("$1T result = new $1T()", hashMap);

            for (var utility : utilities) {
                code.addStatement("result.put($1T.class, new $2T(world))", utility.source.className(), utility.className());
            }

            code.add(System.lineSeparator());
            code.addStatement("return result");

            return MethodSpec.methodBuilder("getUtilities")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ClassName.OBJECT, "arg")
                    .returns(returnType)
                    .addCode(code.build())
                    .build();
        }

        private MethodSpec createArchetypeEntityData() {
            var components = ParameterizedTypeName.get(ClassName.get(List.class), Utils.CLASS_WILDCARD);

            return MethodSpec.methodBuilder("createArchetypeEntityData")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(TypeName.INT, "entityBagSize")
                    .addParameter(components, "components")
                    .returns(Object.class)
                    .addStatement("return $1T.create(entityBagSize, components)", Utils.ENTITY_ARCHETYPE_DATA_IMPL)
                    .build();
        }

        private MethodSpec set() {
            var typeT = TypeVariableName.get("T");
            var returnType = ParameterizedTypeName.get(ClassName.get(Set.class), typeT);
            var hashSet = ParameterizedTypeName.get(ClassName.get(HashSet.class), typeT);

            var code = CodeBlock.builder()
                    .addStatement("$1T result = new $1T()", hashSet)
                    .beginControlFlow("for (T value : values)")
                    .addStatement("result.add(value)")
                    .endControlFlow()
                    .add(System.lineSeparator())
                    .addStatement("return result")
                    .build();

            return MethodSpec.methodBuilder("set")
                    .addAnnotation(SafeVarargs.class)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .addTypeVariable(typeT)
                    .addParameter(ArrayTypeName.of(typeT), "values").varargs()
                    .returns(returnType)
                    .addCode(code)
                    .build();
        }

    }

    private class SystemImplementation {

        public SystemJavaType generate(SystemData system) {
            // TODO more tests for potential name clashes everywhere
            // TODO utilities / systems / World only used by @EntityProcessor need no fields

            var className = ClassName.get("", system.className().simpleName() + "Impl");
            var names = new HashMap<String, Integer>();

            var fieldProviders = Stream.concat(system.methods().stream(), system.callbacks().stream())
                    .flatMap(method -> method.parameters().stream())
                    .filter(SystemParameterData.class::isInstance)
                    .filter(FieldProvider.class::isInstance)
                    .map(FieldProvider.class::cast)
                    .filter(Utils.distinctBy(FieldProvider::fieldSpec))
                    .toList();

            var fields = fieldProviders.stream().map(FieldProvider::fieldSpec).toList();

            var processorData = processor.generate(className, system, names);
            var insertedData = inserted.generate(className, system, names);
            var removedData = removed.generate(className, system, names);
            var utilities = getUtilityResults(system, names, false);

            var type = TypeSpec.classBuilder(className)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(system.className())
                    .addSuperinterface(Utils.SYSTEM_TYPE)
                    .addMethod(constructor(system, fieldProviders, processorData, insertedData, removedData, utilities))
                    .addMethod(offerArchetype(system, processorData, utilities))
                    .addMethod(runSystem(system, processorData, utilities))
                    .addField(Utils.INTERNAL_WORLD, "_world", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(fields)
                    .addFields(processorData.fields())
                    .addFields(insertedData.fields())
                    .addFields(removedData.fields())
                    .addFields(utilities.stream().flatMap(result -> result.fields().stream()).toList())
                    .addMethods(insertedData.methods())
                    .addMethods(removedData.methods())
                    .addMethods(utilities.stream().flatMap(result -> result.methods().stream()).toList())
                    .addTypes(processorData.types())
                    .addTypes(insertedData.types())
                    .addTypes(removedData.types())
                    .addTypes(utilities.stream().flatMap(result -> result.types().stream()).toList())
                    .build();

            var dependencies = fieldProviders.stream()
                    .filter(SingletonParameter.class::isInstance)
                    .map(FieldProvider::type)
                    .collect(Collectors.toSet());

            var staticImports = processStaticImports(system);

            return new SystemJavaType(system, dependencies, system.className().packageName(), type, staticImports);
        }

        private static List<StaticImport> processStaticImports(SystemData system) {
            var classes = new HashMap<ClassName, List<String>>();

            for (var m : system.methods()) {
                if (!(m instanceof EntityProcessorMethod method) || method.source() == null) {
                    continue;
                }

                var source = method.source();
                for (var name : source.staticImports()) {
                    var lastDot = name.lastIndexOf('.');
                    var className = ClassName.bestGuess(name.substring(0, lastDot));

                    var names = classes.computeIfAbsent(className, ignore -> new ArrayList<>());
                    names.add(name.substring(lastDot + 1));
                }
            }

            return classes.entrySet().stream()
                    .map(entry -> new StaticImport(entry.getKey(), entry.getValue().toArray(String[]::new)))
                    .toList();
        }

        private MethodSpec constructor(SystemData system, List<FieldProvider> fieldProviders, ProcessorResult processorData, GeneratorResult insertedData, GeneratorResult removedData,
                                       List<GeneratorResult> utilities) {
            var code = CodeBlock.builder();
            code.addStatement("this._world = world");

            for (var fieldProvider : fieldProviders) {
                fieldProvider.fieldInit(code, "world", null);
            }

            code.add(processorData.fieldInit().build());

            for (var utility : utilities) {
                code.add(utility.fieldInit().build());
            }

            code.add(insertedData.fieldInit().build());
            code.add(removedData.fieldInit().build());

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Utils.INTERNAL_WORLD, "world")
                    .addCode(code.build())
                    .build();
        }

        private MethodSpec offerArchetype(SystemData system, ProcessorResult processorData, List<GeneratorResult> utilities) {
            var code = CodeBlock.builder();

            for (var type : processorData.types()) {
                var className = ClassName.get("", type.name());
                var fieldName = Utils.decapitalize(type.name());

                code.beginControlFlow("if ($1T.COMPOSITION.matches(archetype))", className);
                code.addStatement("this.%s.add(new $1T(this._world, archetype, this))".formatted(fieldName), className);

                // TODO naming convention in two places is not nice
                if (processorData.fields().stream().anyMatch(field -> field.name().equals(fieldName + "Futures"))) {
                    code.addStatement("this.%sFutures.add(null)".formatted(fieldName));
                }

                code.endControlFlow();
            }

            for (var utility : utilities) {
                code.add(utility.offerArchetypeInit().build());
            }

            return MethodSpec.methodBuilder("offerArchetype")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(Utils.ENTITY_ARCHETYPE, "archetype")
                    .addCode(code.build())
                    .build();
        }

        private MethodSpec runSystem(SystemData system, ProcessorResult processorData, List<GeneratorResult> utilities) {
            var code = CodeBlock.builder();

            if (!utilities.isEmpty()) {
                for (var utility : utilities) {
                    code.add(utility.runInit().build());
                }

                code.add(System.lineSeparator());
            }

            code.add(processorData.runImpl().build());

            return MethodSpec.methodBuilder("runSystem")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addCode(code.build())
                    .build();
        }

    }

    private class UtilityImplementation {

        public SystemJavaType generate(UtilityData utility) {
            var className = ClassName.get("", utility.className().simpleName() + "Impl");
            var names = new HashMap<String, Integer>();

            var utilities = getUtilityResults(utility, names, true);
            var requiresWorld = utilities.stream().anyMatch(result -> result.requiresWorld().get());

            var type = TypeSpec.classBuilder(className)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(utility.className())
                    .addSuperinterface(Utils.UTILITY_TYPE)
                    .addMethod(constructor(utilities, requiresWorld))
                    .addMethod(offerArchetype(utility, utilities))
                    .addFields(createFields(utilities, requiresWorld))
                    .addMethods(utilities.stream().flatMap(result -> result.methods().stream()).toList())
                    .addTypes(utilities.stream().flatMap(result -> result.types().stream()).toList())
                    .build();

            return new SystemJavaType(utility, Set.of(), utility.className().packageName(), type, List.of());
        }

        private List<FieldSpec> createFields(List<GeneratorResult> utilities, boolean requiresWorld) {
            var fields = new ArrayList<FieldSpec>();
            if (requiresWorld) {
                fields.addFirst(FieldSpec.builder(Utils.INTERNAL_WORLD, "_world", Modifier.PRIVATE, Modifier.FINAL).build());
            }

            utilities.stream().flatMap(result -> result.fields().stream()).forEach(fields::add);

            return fields;
        }

        private MethodSpec constructor(List<GeneratorResult> utilities, boolean requiresWorld) {
            var code = CodeBlock.builder();
            if (requiresWorld) {
                code.addStatement("this._world = world");
            }

            for (var utility : utilities) {
                code.add(utility.fieldInit().build());
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Utils.INTERNAL_WORLD, "world")
                    .addCode(code.build())
                    .build();
        }

        private MethodSpec offerArchetype(UtilityData utility, List<GeneratorResult> utilities) {
            var code = CodeBlock.builder();

            for (var method : utilities) {
                code.add(method.offerArchetypeInit().build());
            }

            return MethodSpec.methodBuilder("offerArchetype")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(Utils.ENTITY_ARCHETYPE, "archetype")
                    .addCode(code.build())
                    .build();
        }

    }

    private List<GeneratorResult> getUtilityResults(TypeData type, Map<String, Integer> names, boolean utility) {
        return type.utils().stream()
                .collect(Collectors.groupingBy(Object::getClass))
                .values().stream()
                .map(methods -> switch (methods.getFirst()) {
                    case ArchetypeMethod method ->
                            archetype.generate(type, methods.stream().map(ArchetypeMethod.class::cast).toList(), names, utility);
                    case TransmuteMethod method ->
                            transmute.generate(type, methods.stream().map(TransmuteMethod.class::cast).toList(), names);
                    case CountMethod method ->
                            count.generate(methods.stream().map(CountMethod.class::cast).toList(), names);
                })
                .toList();
    }

    private class Detector {

        public Set<TypeData> detectTypes() {
            var data = new SystemDataHolder();

            // Detect @SystemProcessor and @EntityProcessor
            processor.detect().reduce(data, data::accumulate, data::combine);

            // Detect @Inserted1
            inserted.detect().reduce(data, data::accumulate, data::combine);

            // Detect @Removed
            removed.detect().reduce(data, data::accumulate, data::combine);

            // Detect @Count
            count.detect().reduce(data, data::accumulate, data::combine);

            // Detect @Archetype
            archetype.detect().reduce(data, data::accumulate, data::combine);

            // Detect @Transmute
            transmute.detect().reduce(data, data::accumulate, data::combine);

            // Validate types
            var types = Set.copyOf(data.types.values());

            var utilities = types.stream().filter(UtilityData.class::isInstance).map(UtilityData.class::cast).toList();
            validateUtilityTypes(utilities);

            if (isError()) {
                return Set.of();
            }

            return types;
        }

        private void validateUtilityTypes(List<UtilityData> types) {
            var archetypeError = false;
            var transuteError = false;
            var countError = false;

            // Must be interfaces
            for (var type : types) {
                if (!type.element().getModifiers().contains(Modifier.ABSTRACT) || type.element().getKind() != ElementKind.CLASS) {
                    for (var method : type.utils()) {
                        switch (method) {
                            case ArchetypeMethod m -> {
                                if (!archetypeError) {
                                    archetypeError = true;
                                    printError("Types using @Archetype without system annotations must be an abstract class.", type.element());
                                }
                            }
                            case TransmuteMethod m -> {
                                if (!transuteError) {
                                    transuteError = true;
                                    printError("Types using @Transmute without system annotations must be an abstract class.", type.element());
                                }
                            }
                            case CountMethod m -> {
                                if (!countError) {
                                    countError = true;
                                    printError("Types using @Count without system annotations must be an abstract class.", type.element());
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private static class Manifest {

        private static final Comparator<TypeData> COMPARATOR_TYPE_DATA = Comparator.comparing(TypeData::className);

        private static final String DIR = "META-INF/decs/types/";
        private static final String MANIFEST = DIR + "manifest";

        public static Set<TypeData> getTypes(Set<TypeData> current, SystemGenerator generator) {
            // Read manifest
            var currentTypes = current.stream().map(type -> type.className().toString()).collect(Collectors.toSet());
            var types = readManifest(generator, currentTypes);

            // Remove values in current round
            types.removeIf(existing -> current.stream().map(TypeData::className).anyMatch(existing.className()::equals));

            // Add values from current round
            types.addAll(current);

            // Write updated manifest
            writeManifest(types, generator);

            return types;
        }

        private static Set<TypeData> readManifest(SystemGenerator generator, Set<String> currentTypes) {
            var filer = generator.processingEnv.getFiler();
            var result = new TreeSet<>(COMPARATOR_TYPE_DATA);

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
                            try {
                                result.add(TypeData.readMetadata(reader));
                            } catch (RuntimeException ex) {
                                var writer = new StringWriter();
                                ex.printStackTrace(new PrintWriter(writer));

                                generator.printError("Failed to parse system metadata for '%s' (line %d): %s%s%s".formatted(line, reader.getLineNumber(), ex.getMessage(), System.lineSeparator(), writer.toString()));
                            }
                        } catch (NoSuchFileException ex) {
                            if (!currentTypes.contains(line)) {
                                generator.printError("Failed to read system metadata for '%s': No such file (NoSuchFileException)".formatted(line));
                                return result;
                            }
                        } catch (IOException ex) {
                            // Eclipse JDT does not throw NoSuchFileException
                            if (ex.getMessage().contains("does not exist")) {
                                if (!currentTypes.contains(line)) {
                                    generator.printError("Failed to read system metadata for '%s': No such file (IOException)".formatted(line));
                                }

                                continue;
                            }

                            var writer = new StringWriter();
                            ex.printStackTrace(new PrintWriter(writer));

                            generator.printError("Failed to read system metadata for '%s': %s%s%s".formatted(line, ex.getMessage(), System.lineSeparator(), writer.toString()));
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

                generator.printError("Failed to read types manifest: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
                return result;
            }
        }

        private static void writeManifest(Set<TypeData> types, SystemGenerator generator) {
            var filer = generator.processingEnv.getFiler();

            var originatingElements = types.stream()
                    .map(TypeData::element)
                    .filter(Objects::nonNull)
                    .toArray(Element[]::new);

            try {
                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", MANIFEST, originatingElements);

                try (var writer = file.openWriter()) {
                    for (var typeData : types) {
                        writer.append(typeData.className().canonicalName()).append(System.lineSeparator());
                        writeMetadata(typeData, generator);
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write type manifest: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
            }
        }

        private static void writeMetadata(TypeData typeData, SystemGenerator generator) {
            var fqcn = typeData.className().canonicalName();
            var filer = generator.processingEnv.getFiler();

            try {
                var resourcePath = "META-INF/decs/types/%s".formatted(typeData.className().canonicalName());
                var originatingElements = typeData.element() != null
                        ? new Element[]{typeData.element()}
                        : new Element[0];

                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath, originatingElements);

                try (var writer = file.openWriter()) {
                    typeData.writeMetadata(writer);
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write system metadata for '%s': %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()), typeData.element());
            }
        }

    }

    private record SystemDataHolder(Map<TypeElement, TypeData> types) {

        public SystemDataHolder() {
            this(new HashMap<>());
        }

        private SystemDataHolder accumulate(SystemDataHolder holder, TypeData type) {
            if (type != null) {
                holder.types.merge(type.element(), type, TypeData::merge);
            }

            return holder;
        }

        private SystemDataHolder combine(SystemDataHolder first, SystemDataHolder second) {
            throw new UnsupportedOperationException("No parallel streams used.");
        }

    }

    private record SystemJavaType(TypeData source, Set<TypeName> dependencies, String packageName,
                                  TypeSpec type, List<StaticImport> staticImports) implements JavaType {
    }

}
