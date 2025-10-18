package de.schosin.decs.codegen.components;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentData.ClassComponent;
import de.schosin.decs.codegen.components.ComponentData.EnumComponent;
import de.schosin.decs.codegen.components.ComponentData.SingletonEnumComponent;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Collectors;

public class ComponentsGenerator extends AbstractGenerator {

    private static final String COMPONENT_TYPE_CLASS = "CLASS";
    private static final String COMPONENT_TYPE_ENUM = "ENUM";
    private static final String COMPONENT_TYPE_SINGLETON = "SINGLETON";

    private static final String PACKAGE = Utils.COMPONENTS.packageName();
    private static final String METADATA_NAME = "ComponentMetadata";
    private static final String COMPONENTS_NAME = Utils.COMPONENTS.simpleName();

    public ComponentsGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components) {
        super(processingEnv, roundEnv, components);
    }

    public void process() {
        // Process types
        roundEnv.getElementsAnnotatedWith(annotations.component()).forEach(this::processComponent);

        // Read components not in current round from manifest
        Manifest.readComponentsFromManifest(this);
    }

    private void processComponent(Element element) {
        if (!(element instanceof TypeElement component)) {
            printError("Components must be a class or an enum.", element);
            return;
        }

        if (component.getQualifiedName().toString().equals(element.getSimpleName().toString())) {
            printError("Components must not be in the unnamed package.", element);
            return;
        }

        var parent = component.getEnclosingElement();
        if (parent != null && parent.getKind() != ElementKind.PACKAGE) {
            printError("Components must not be nested types.", element);
            return;
        }

        switch (component.getKind()) {
            case CLASS -> processClassComponent(component);
            case ENUM -> processEnumComponent(component);
            default -> printError("Components must be a class or an enum.", element);
        }
    }

    private void processClassComponent(TypeElement component) {
        // Validate public / public static when nested for zero-reflection access
        if (!isPublicAccessible(component)) {
            return;
        }

        // Validate public default constructor present
        if (!hasPublicDefaultConstructor(component)) {
            printError("Components require a public default constructor.", component);
            return;
        }

        // Create data
        var type = (DeclaredType) component.asType();
        this.components.components().put(type.toString(), new ClassComponent(type, component, ClassName.get(component)));
    }

    private boolean hasPublicDefaultConstructor(TypeElement type) {
        // No constructors
        var noConstructors = type.getEnclosedElements().stream().noneMatch(element -> element.getKind() == ElementKind.CONSTRUCTOR);
        if (noConstructors) {
            return true;
        }

        // Explicit default constructor
        return type.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(constructor -> constructor.getModifiers().contains(Modifier.PUBLIC) && constructor.getParameters().isEmpty());
    }

    private void processEnumComponent(TypeElement component) {
        // Validate public / public static when nested for zero-reflection access
        if (!isPublicAccessible(component)) {
            return;
        }

        // Resolve enum values
        var values = component.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.ENUM_CONSTANT)
                .toList();

        if (values.isEmpty()) {
            printError("Enum components must have atleast one enum constant.", component);
            return;
        }

        // Create data
        var type = (DeclaredType) component.asType();

        var data = values.size() == 1
                ? new SingletonEnumComponent(type, component, ClassName.get(component), values.getFirst().getSimpleName().toString())
                : new EnumComponent(type, component, ClassName.get(component));

        this.components.components().put(type.toString(), data);
    }

    public List<JavaType> generate() {
        // Generate component metadata
        this.components.components().values().forEach(component -> Manifest.generateMetadata(component, this));

        // Write manifest
        Manifest.writeManifest(this);

        // Validate simple names are unique
        for (var component : this.components.components().values()) {
            this.components.simpleNames().computeIfAbsent(component.className().simpleName(), ignore -> new ArrayList<>()).add(component.element());
        }

        for (var types : this.components.simpleNames().values()) {
            if (types.size() == 1) {
                continue;
            }

            for (var type : types) {
                var others = types.stream().filter(it -> it != type).map(TypeElement::getQualifiedName).map(Name::toString).collect(Collectors.joining(", "));
                printError("Components must have a unique simple name. Collision with: %s".formatted(others), type);
            }
        }

        // Note for unused components
        for (var component : this.components.components().values()) {
            var selects = this.components.getReads(component.type());
            var reads = this.components.getReads(component.type());
            var writes = this.components.getWrites(component.type());

            if (writes == 0 && (selects > 0 || reads > 0)) {
                printWarning("Component is never being added to an entity.", component.element());
                continue;
            }

            if (writes > 0 && selects == 0 && reads == 0) {
                printWarning("Component is being added to entities, but not used by entity processors.", component.element());
                continue;
            }

            if (selects == 0 && reads == 0 && writes == 0) {
                printNote("Component is not used.", component.element());
            }
        }

        // Prepare components
        var sortedComponents = new ArrayList<>(this.components.components().values());
        sortedComponents.sort((first, second) -> sortComponents(first, second, this.components));

        this.components.sortedComponents().addAll(sortedComponents);

        var result = new ArrayList<JavaType>();
        result.add(Components.create(sortedComponents));
        result.add(ComponentMetadata.create());

        return result;
    }

    private int sortComponents(ComponentData first, ComponentData second, ComponentsResult result) {
        // sort singleton enums to the back as they will be directly inlined
        if (first instanceof SingletonEnumComponent) {
            if (!(second instanceof SingletonEnumComponent)) {
                return 1;
            }
        }

        if (second instanceof SingletonEnumComponent) {
            if (!(first instanceof SingletonEnumComponent)) {
                return -1;
            }
        }

        // sort by number of reads descending (used more often -> lower id)
        var reads = result.getReads(second.type()) - result.getReads(first.type());
        if (reads != 0) {
            return reads;
        }

        // sort by number of writes descending (used more often -> lower id)
        return result.getWrites(second.type()) - result.getWrites(first.type());
    }

    private static class Components {

        public static JavaType create(List<ComponentData> components) {
            var type = TypeSpec.classBuilder(COMPONENTS_NAME)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(lookup(components))
                    .addStaticBlock(lookupInitializer(components))
                    .addMethod(getMetadata())
                    .build();

            return JavaType.create(PACKAGE, type);
        }

        private static FieldSpec lookup(List<ComponentData> components) {
            var metadata = ParameterizedTypeName.get(ClassName.get("", METADATA_NAME), Utils.WILDCARD);
            var map = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, metadata);

            return FieldSpec.builder(map, "LOOKUP", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build();
        }

        private static CodeBlock lookupInitializer(List<ComponentData> components) {
            var metadataRaw = ClassName.get("", METADATA_NAME);
            var metadata = ParameterizedTypeName.get(metadataRaw, Utils.WILDCARD);
            var hashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, metadata);

            var code = CodeBlock.builder();
            code.addStatement("$1T result = new $1T()", hashMap);

            for (int i = 0, s = components.size(); i < s; i++) {
                var component = components.get(i);

                switch (component) {
                    case ClassComponent c ->
                            code.addStatement("result.put($2T.class, new $1T<>(%d, $2T.class, $3T.unbounded(1024, $2T.class, $2T::new)))"
                                    .formatted(i), metadataRaw, component.className(), Utils.POOL);
                    case ComponentData.EnumComponentData c ->
                            code.addStatement("result.put($2T.class, new $1T<>(%d, $2T.class, null))"
                                    .formatted(i), metadataRaw, component.className());
                }
            }

            code.add(System.lineSeparator());
            code.addStatement("LOOKUP = $1T.unmodifiableMap(result)", Collections.class);

            return code.build();
        }

        private static MethodSpec getMetadata() {
            var typeT = TypeVariableName.get("T");
            var classType = ParameterizedTypeName.get(ClassName.get(Class.class), typeT);
            var metadata = ParameterizedTypeName.get(ClassName.get("", METADATA_NAME), typeT);

            var code = CodeBlock.builder()
                    .addStatement("$1T result = ($1T) LOOKUP.get(type)", metadata)
                    .beginControlFlow("if (result != null)")
                    .addStatement("return result")
                    .endControlFlow()
                    .add(System.lineSeparator())
                    .addStatement("throw new $1T(String.format(\"Unknown component type: %s\", type.getName()))", IllegalArgumentException.class)
                    .build();

            return MethodSpec.methodBuilder("getMetadata")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(typeT)
                    .addParameter(classType, "type")
                    .returns(metadata)
                    .addCode(code)
                    .build();
        }
    }

    private static class ComponentMetadata {

        public static JavaType create() {
            var typeT = TypeVariableName.get("T");
            var classType = ParameterizedTypeName.get(ClassName.get(Class.class), typeT);

            var type = TypeSpec.classBuilder(METADATA_NAME)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariable(typeT)
                    .addField(comparator())
                    .addField(TypeName.INT, "id", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(classType, "type", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Object.class, "pool")
                    .addMethod(constructor(classType))
                    .addMethod(getId())
                    .addMethod(getType(classType))
                    .addMethod(getPool())
                    .build();

            return JavaType.create(PACKAGE, type);
        }

        private static FieldSpec comparator() {
            var metadata = ClassName.get("", METADATA_NAME);
            var type = ParameterizedTypeName.get(ClassName.get(Comparator.class), metadata);

            return FieldSpec.builder(type, "COMPARATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.comparingInt($2T::getId)", Comparator.class, metadata)
                    .build();
        }

        private static MethodSpec constructor(ParameterizedTypeName classType) {
            var code = CodeBlock.builder()
                    .addStatement("this.id = id")
                    .addStatement("this.type = type")
                    .addStatement("this.pool = pool")
                    .build();

            return MethodSpec.constructorBuilder()
                    .addParameter(TypeName.INT, "id")
                    .addParameter(classType, "type")
                    .addParameter(Object.class, "pool")
                    .addCode(code)
                    .build();
        }

        private static MethodSpec getId() {
            return MethodSpec.methodBuilder("getId")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName.INT)
                    .addStatement("return this.id")
                    .build();
        }

        private static MethodSpec getType(ParameterizedTypeName classType) {
            return MethodSpec.methodBuilder("getType")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(classType)
                    .addStatement("return this.type")
                    .build();
        }

        private static MethodSpec getPool() {
            return MethodSpec.methodBuilder("getPool")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(Object.class)
                    .addStatement("return this.pool")
                    .build();
        }

    }

    private static class Manifest {

        public static void generateMetadata(ComponentData component, ComponentsGenerator generator) {
            var filer = generator.processingEnv.getFiler();

            try {
                var resourcePath = "META-INF/decs/components/%s".formatted(component.className().canonicalName());
                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath, component.element());

                try (var writer = file.openWriter()) {
                    switch (component) {
                        case ClassComponent c -> {
                            writer.append(COMPONENT_TYPE_CLASS);
                            writer.append(";").append(c.className().packageName());
                            writer.append(";").append(c.className().simpleName());
                        }
                        case EnumComponent c -> {
                            writer.append(COMPONENT_TYPE_ENUM);
                            writer.append(";").append(c.className().packageName());
                            writer.append(";").append(c.className().simpleName());
                        }
                        case SingletonEnumComponent c -> {
                            writer.append(COMPONENT_TYPE_SINGLETON);
                            writer.append(";").append(c.className().packageName());
                            writer.append(";").append(c.className().simpleName());
                            writer.append(";").append(c.instance());
                        }
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write metadata: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()), component.element());
            }
        }

        private static ComponentData readMetadata(String fqcn, ComponentsGenerator generator) {
            var filer = generator.processingEnv.getFiler();

            try {
                var resourcePath = "META-INF/decs/components/%s".formatted(fqcn);
                var file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);

                try (var reader = new BufferedReader(new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8))) {
                    var line = reader.readLine();
                    var split = line.split(";");

                    var type = split[0];
                    if (split.length < 3) {
                        generator.printError("Failed to read metadata for '%s': Invalid format (type '%s', %d sections), do a full rebuild.".formatted(fqcn, type, split.length));
                        return null;
                    }

                    var className = ClassName.get(split[1], split[2]);
                    var element = generator.elements.getTypeElement(className.canonicalName());
                    var declaredType = (DeclaredType) element.asType();

                    switch (type) {
                        case COMPONENT_TYPE_CLASS -> {
                            if (split.length != 3) {
                                generator.printError("Failed to read metadata for '%s': Invalid format (type '%s', %d sections), do a full rebuild.".formatted(fqcn, type, split.length));
                                return null;
                            }

                            return new ClassComponent(declaredType, element, className);
                        }
                        case COMPONENT_TYPE_ENUM -> {
                            return new EnumComponent(declaredType, element, className);
                        }
                        case COMPONENT_TYPE_SINGLETON -> {
                            if (split.length != 4) {
                                generator.printError("Failed to read metadata for '%s': Invalid format (type '%s', %d sections), do a full rebuild.".formatted(fqcn, type, split.length));
                                return null;
                            }

                            var instance = split[3];
                            return new SingletonEnumComponent(declaredType, element, className, instance);
                        }
                        default -> {
                            generator.printError("Failed to read metadata for '%s': Unknown type '%s', do a full rebuild.".formatted(fqcn, type));
                            return null;
                        }
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read metadata for '%s': %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()));
                return null;
            }
        }

        public static void readComponentsFromManifest(ComponentsGenerator generator) {
            // Read all components from manifest
            var components = readManifest(generator);
            if (components == null) {
                return;
            }

            // Remove types in current round (will remove types that had their @Component removed)
            components.removeIf(type -> generator.components.components().values().stream()
                    .anyMatch(component -> component.type().toString().equals(type.toString())));

            // Add components from current round
            for (var component : components) {
                generator.components.components().put(component.type().toString(), component);
            }
        }

        private static Set<ComponentData> readManifest(ComponentsGenerator generator) {
            var filer = generator.processingEnv.getFiler();
            var result = new HashSet<ComponentData>();

            try {
                var resourcePath = "META-INF/decs/components/manifest";
                var file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);

                try (var reader = new BufferedReader(new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) {
                            var metadata = readMetadata(line, generator);
                            if (metadata != null) {
                                result.add(metadata);
                            }
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

                generator.printError("Failed to read components metadata: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
                return result;
            }
        }

        public static void writeManifest(ComponentsGenerator generator) {
            var filer = generator.processingEnv.getFiler();

            try {
                var resourcePath = "META-INF/decs/components/manifest";
                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);

                try (var writer = file.openWriter()) {
                    for (var component : generator.components.components().values()) {
                        writer.append(component.className().canonicalName());
                        writer.append(System.lineSeparator());
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read components metadata: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
            }
        }

    }

}
