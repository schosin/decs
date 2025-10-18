package de.schosin.decs.codegen.components;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentData.*;
import de.schosin.decs.codegen.entityarchetype.EntityArchetypeDataGenerator;
import de.schosin.decs.codegen.entityarchetype.EntityArchetypeDataGenerator.InterfaceComponentField;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ComponentsGenerator extends AbstractGenerator {

    // "reset" because implementation implements Pooled
    private static final Set<String> DISALLOWED_INTERFACE_FIELD_NAMES = Set.of("hashCode", "toString", "equals", "reset");

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
            printError("Components must be a class, enum or interface: " + element.getKind(), element);
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

        if (!component.getTypeParameters().isEmpty()) {
            printError("Components must not be generic.", element);
            return;
        }

        switch (component.getKind()) {
            case CLASS -> processClassComponent(component);
            case ENUM -> processEnumComponent(component);
            case INTERFACE -> processInterfaceComponent(component);
            default -> printError("Components must be a class, enum or interface: " + element.getKind(), element);
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
        var data = new ClassComponent(type, component, ClassName.get(component));

        this.components.add(data);
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

        this.components.add(data);
    }

    private void processInterfaceComponent(TypeElement component) {
        // Validate public / public static when nested for zero-reflection access
        if (!isPublicAccessible(component)) {
            return;
        }

        // Resolve component fields based on methods
        var fields = resolveInterfaceComponentFields(component);
        if (fields == null) {
            return;
        }

        if (fields.isEmpty()) {
            printError("Interface components must have atleast one field.", component);
            return;
        }

        // Create data
        var type = (DeclaredType) component.asType();
        var data = new InterfaceComponent(type, component, ClassName.get(component), fields);

        this.components.add(data);
    }

    private List<Parameter> resolveInterfaceComponentFields(TypeElement component) {
        // Fields are declared in pairs of methods (getter and setter), so group methods by their name
        var grouped = component.getEnclosedElements().stream()
                .filter(elem -> elem.getKind() == ElementKind.METHOD)
                .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                .map(ExecutableElement.class::cast)
                .collect(Collectors.groupingBy(method -> method.getSimpleName().toString()));

        var result = new ArrayList<Parameter>(grouped.size());
        var error = false;

        for (var entry : grouped.entrySet()) {
            var fieldError = false;

            var name = entry.getKey();
            var group = entry.getValue();

            // Disallow certain names
            if (DISALLOWED_INTERFACE_FIELD_NAMES.contains(name)) {
                for (var method : group) {
                    printError("Cannot use '%s' as an interface component field name.".formatted(name));
                }

                error = true;
                continue;
            }

            // Validate exactly two methods (getter and setter)
            if (group.size() != 2) {
                for (var method : group) {
                    printError("Abstract methods in interface methods must be a getter/setter pair. The name '%s' has %d methods, only 2 expected.".formatted(name, group.size()));
                }

                error = true;
                continue;
            }

            // Validate first method
            var method1 = group.get(0);
            var getterType1 = getInterfaceFieldGetterType(method1);
            var setterType1 = getInterfaceFieldSetterType(method1);

            if ((getterType1 != null) == (setterType1 != null)) {
                printError("Method '%s' is neither a getter (non-void, no parameters) or a setter (void, one parameter)".formatted(name), method1);
                fieldError = true;
            }

            var method2 = group.get(1);
            var getterType2 = getInterfaceFieldGetterType(method2);
            var setterType2 = getInterfaceFieldSetterType(method2);

            if ((getterType2 != null) == (setterType2 != null)) {
                printError("Method '%s' is neither a getter (non-void, no parameters) or a setter (void, one parameter)".formatted(name), method2);
                fieldError = true;
            }

            var type1 = getterType1 != null ? getterType1 : setterType1;
            var type2 = getterType2 != null ? getterType2 : setterType2;
            if (!Objects.equals(type1, type2)) {
                printError("Methods '%s' must have the same type", method1);
                printError("Methods '%s' must have the same type", method2);
                fieldError = true;
            }

            if (fieldError) {
                error = true;
                continue;
            }

            result.add(new Parameter(type1, name));
        }

        if (error) {
            return null;
        }

        return result;
    }

    private static TypeName getInterfaceFieldGetterType(ExecutableElement method) {
        if (!method.getParameters().isEmpty()) {
            return null;
        }

        var returnType = method.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
            return null;
        }

        return TypeName.get(returnType);
    }

    private static TypeName getInterfaceFieldSetterType(ExecutableElement method) {
        if (method.getParameters().size() != 1) {
            return null;
        }

        var returnType = method.getReturnType();
        if (returnType.getKind() != TypeKind.VOID) {
            return null;
        }

        return TypeName.get(method.getParameters().getFirst().asType());
    }

    public List<JavaType> generate() {
        // Generate component metadata
        this.components.components().values().stream()
                .filter(component -> component.element() != null) // only current round
                .forEach(component -> Manifest.generateMetadata(component, this));

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
            var selects = this.components.getReads(component);
            var reads = this.components.getReads(component);
            var writes = this.components.getWrites(component);

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
        result.addAll(this.components.sortedComponents().stream()
                .filter(component -> component.element() != null) // only current round
                .filter(InterfaceComponent.class::isInstance)
                .map(InterfaceComponent.class::cast)
                .map(data -> ComponentImpl.create(data, this))
                .toList());

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
        var reads = result.getReads(second) - result.getReads(first);
        if (reads != 0) {
            return reads;
        }

        // sort by number of writes descending (used more often -> lower id)
        return result.getWrites(second) - result.getWrites(first);
    }

    private static class ComponentImpl {

        public static JavaType create(InterfaceComponent data, AbstractGenerator generator) {
            var type = TypeSpec.classBuilder(data.impl())
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(data.className())
                    .addSuperinterface(Utils.POOLED)
                    .addField(TypeName.INT, "index", Modifier.PUBLIC)
                    .addFields(fields(data, generator))
                    .addMethod(constructor(data, generator))
                    .addMethods(implementations(data, generator))
                    .addMethod(reset())
                    .build();

            return JavaType.create(data.impl().packageName(), type);
        }


        private static List<FieldSpec> fields(InterfaceComponent data, AbstractGenerator generator) {
            return data.fields().stream()
                    .map(field -> EntityArchetypeDataGenerator.createInterfaceComponentField(data, field, generator))
                    .map(InterfaceComponentField::field)
                    .toList();
        }

        private static MethodSpec constructor(InterfaceComponent data, AbstractGenerator generator) {
            var code = CodeBlock.builder();
            code.addStatement("this.index = -1");

            code.add(System.lineSeparator());
            for (var field : data.fields()) {
                var fieldSpec = EntityArchetypeDataGenerator.createInterfaceComponentField(data, field, generator);

                code.addStatement("this.%1$s = data.%1$s".formatted(fieldSpec.field().name()));
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Utils.ENTITY_ARCHETYPE_DATA_IMPL, "data")
                    .addCode(code.build())
                    .build();
        }

        private static List<MethodSpec> implementations(InterfaceComponent data, AbstractGenerator generator) {
            return data.fields().stream()
                    .flatMap(field -> createImplementation(data, field, generator))
                    .toList();
        }

        private static Stream<MethodSpec> createImplementation(InterfaceComponent data, Parameter field, AbstractGenerator generator) {
            var name = field.name();
            var fieldSpec = EntityArchetypeDataGenerator.createInterfaceComponentField(data, field, generator);

            var getter = MethodSpec.methodBuilder(name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(field.type())
                    .addStatement("return this.%s.getUnsafe(index)".formatted(fieldSpec.field().name()))
                    .build();

            var setter = MethodSpec.methodBuilder(name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(field.type(), "value")
                    .addStatement("this.%s.setUnsafe(index, value)".formatted(fieldSpec.field().name()))
                    .build();

            return Stream.of(getter, setter);
        }

        private static MethodSpec reset() {
            return MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addStatement("this.index = -1")
                    .build();
        }

    }

    private static class Components {

        public static JavaType create(List<ComponentData> components) {
            var type = TypeSpec.classBuilder(Utils.COMPONENTS)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(lookup(components))
                    .addStaticBlock(lookupInitializer(components))
                    .addMethod(getMetadata())
                    .build();

            return JavaType.create(Utils.COMPONENTS.packageName(), type);
        }

        private static FieldSpec lookup(List<ComponentData> components) {
            var metadata = ParameterizedTypeName.get(Utils.COMPONENT_METADATA, Utils.WILDCARD);
            var map = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, metadata);

            return FieldSpec.builder(map, "LOOKUP", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build();
        }

        private static CodeBlock lookupInitializer(List<ComponentData> components) {
            var metadataRaw = Utils.COMPONENT_METADATA;
            var metadata = ParameterizedTypeName.get(metadataRaw, Utils.WILDCARD);
            var hashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, metadata);

            var code = CodeBlock.builder();
            code.addStatement("$1T result = new $1T()", hashMap);

            for (int i = 0, s = components.size(); i < s; i++) {
                var component = components.get(i);

                switch (component) {
                    case ClassComponent c -> {
                        code.addStatement("result.put($2T.class, new $1T<>(%d, $2T.class, $3T.unbounded(1024, $2T.class, $2T::new), 1))".formatted(i), metadataRaw, component.className(), Utils.POOL);
                    }
                    case EnumComponentData c -> {
                        code.addStatement("result.put($2T.class, new $1T<>(%d, $2T.class, null, 1))".formatted(i), metadataRaw, component.className());
                    }
                    case InterfaceComponent c -> {
                        code.addStatement("result.put($2T.class, new $1T<>(%d, $2T.class, null, %d))".formatted(i, c.fields().size()), metadataRaw, component.className());
                    }
                }
            }

            code.add(System.lineSeparator());
            code.addStatement("LOOKUP = $1T.unmodifiableMap(result)", Collections.class);

            return code.build();
        }

        private static MethodSpec getMetadata() {
            var typeT = TypeVariableName.get("T");
            var classType = ParameterizedTypeName.get(ClassName.get(Class.class), typeT);
            var metadata = ParameterizedTypeName.get(Utils.COMPONENT_METADATA, typeT);

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

            var type = TypeSpec.classBuilder(Utils.COMPONENT_METADATA)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addTypeVariable(typeT)
                    .addField(comparator())
                    .addField(TypeName.INT, "id", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(classType, "type", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Object.class, "pool", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(TypeName.INT, "fields", Modifier.PRIVATE, Modifier.FINAL)
                    .addMethod(constructor(classType))
                    .addMethod(getId())
                    .addMethod(getType(classType))
                    .addMethod(getPool())
                    .addMethod(getFields())
                    .build();

            return JavaType.create(Utils.COMPONENT_METADATA.packageName(), type);
        }

        private static FieldSpec comparator() {
            var type = ParameterizedTypeName.get(ClassName.get(Comparator.class), Utils.COMPONENT_METADATA);

            return FieldSpec.builder(type, "COMPARATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.comparingInt($2T::getId)", Comparator.class, Utils.COMPONENT_METADATA)
                    .build();
        }

        private static MethodSpec constructor(ParameterizedTypeName classType) {
            var code = CodeBlock.builder()
                    .addStatement("this.id = id")
                    .addStatement("this.type = type")
                    .addStatement("this.pool = pool")
                    .addStatement("this.fields = fields")
                    .build();

            return MethodSpec.constructorBuilder()
                    .addParameter(TypeName.INT, "id")
                    .addParameter(classType, "type")
                    .addParameter(Object.class, "pool")
                    .addParameter(TypeName.INT, "fields")
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

        private static MethodSpec getFields() {
            return MethodSpec.methodBuilder("getFields")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName.INT)
                    .addStatement("return this.fields")
                    .build();
        }

    }

    private static class Manifest {

        public static void generateMetadata(ComponentData component, ComponentsGenerator generator) {
            var fqcn = component.className().canonicalName();
            var filer = generator.processingEnv.getFiler();

            try {
                var resourcePath = "META-INF/decs/components/%s".formatted(component.className().canonicalName());
                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath, component.element());

                try (var writer = file.openWriter()) {
                    component.writeMetadata(writer);
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write component metadata for '%s': %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()), component.element());
            } catch (Exception ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write component metadata for '%s' (IO): %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()), component.element());
            }
        }

        private static ComponentData readMetadata(String fqcn, ComponentsGenerator generator) {
            var filer = generator.processingEnv.getFiler();

            try {
                var resourcePath = "META-INF/decs/components/%s".formatted(fqcn);
                var file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);

                try (var reader = new BufferedReader(new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8))) {
                    return ComponentData.readMetadata(reader);
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read component metadata for '%s' (IO): %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()));
                return null;
            } catch (Exception ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read component metadata for '%s': %s%s%s".formatted(fqcn, ex.getMessage(), System.lineSeparator(), writer.toString()));
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
                    .anyMatch(component -> component.className().canonicalName().equals(type.className().canonicalName())));

            // Add components to current round
            for (var component : components) {
                generator.components.add(component);
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

                generator.printError("Failed to read components manifest (IO): %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
                return result;
            } catch (Exception ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read components manifest: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
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

                generator.printError("Failed to write components manifest (IO): %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
            } catch (Exception ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to write components manifest: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
            }
        }

    }

}
