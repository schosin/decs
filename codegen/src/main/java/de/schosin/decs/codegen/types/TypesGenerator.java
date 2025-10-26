package de.schosin.decs.codegen.types;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.system.SystemGenerator.SystemJavaType;
import de.schosin.decs.codegen.system.SystemsResult;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Function;

public class TypesGenerator extends AbstractGenerator {

    private static final String TYPES_PACKAGE = "de.schosin.decs.values";
    private static final String TYPES_NAME = "Types";

    private final List<SystemJavaType> systems;
    private final List<SystemJavaType> utilities;
    private final Map<ClassName, ClassName> systemInvocations;

    public TypesGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components, SystemsResult systems, Map<ClassName, ClassName> systemInvocations) {
        super(processingEnv, roundEnv, components);

        this.systems = systems.systemJavaTypes();
        this.utilities = systems.utilityJavaTypes();
        this.systemInvocations = systemInvocations;
    }

    public List<JavaType> generate() {
        return List.of(
                SystemMetadata.create(),
                TypesImplementation.generate(systems, utilities, systemInvocations)
        );
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

        public static JavaType generate(List<SystemJavaType> systems, List<SystemJavaType> utilities, Map<ClassName, ClassName> systemInvocations) {
            var type = TypeSpec.classBuilder(TYPES_NAME)
                    .addAnnotation(Utils.GENERATED)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(systemsField(systems))
                    .addField(systemInvocationsField())
                    .addStaticBlock(staticInit(systems, systemInvocations))
                    .addMethod(getSystemMetadata(systems))
                    .addMethod(getUtilities(utilities))
                    .addMethod(createArchetypeEntityData())
                    .addMethod(getSystemInvocation())
                    .addMethod(set())
                    .build();

            return JavaType.create(TYPES_PACKAGE, type);
        }


        private static FieldSpec systemsField(List<SystemJavaType> systems) {
            var metadata = ClassName.get("", "SystemMetadata");
            var fieldType = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, metadata);

            return FieldSpec.builder(fieldType, "SYSTEMS", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build();
        }

        private static FieldSpec systemInvocationsField() {
            var constructor = ParameterizedTypeName.get(ClassName.get(Function.class), Utils.INTERNAL_WORLD, Utils.SYSTEM_INVOCATION);
            var fieldType = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, constructor);

            return FieldSpec.builder(fieldType, "INVOCATIONS", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build();
        }

        private static CodeBlock staticInit(List<SystemJavaType> systems, Map<ClassName, ClassName> systemInvocations) {
            var code = CodeBlock.builder();

            // SYSTEMS
            var metadata = ClassName.get("", "SystemMetadata");
            var systemsHashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, metadata);

            code.add("// Initialize SYSTEMS").add(System.lineSeparator());
            code.addStatement("$1T systems = new $1T()", systemsHashMap);

            for (var system : systems) {
                var dependencies = CodeBlock.builder();
                dependencies.add("set(");

                var idx = 0;
                for (var dependency : system.dependencies()) {
                    if (idx++ > 0) {
                        dependencies.add(", ");
                    }

                    dependencies.add("$1T.class", dependency);
                }

                dependencies.add(")");

                code.add("systems.put($1T.class, new $2T($1T.class, $3T.class, invocation -> new $3T(($4T) invocation), ", system.source().className(), metadata, system.className(), Utils.INTERNAL_WORLD);
                code.add(dependencies.build());
                code.addStatement("))");
            }

            code.add(System.lineSeparator());
            code.addStatement("SYSTEMS = $1T.unmodifiableMap(systems)", Collections.class);

            // INVOCATIONS
            var constructor = ParameterizedTypeName.get(ClassName.get(Function.class), Utils.INTERNAL_WORLD, Utils.SYSTEM_INVOCATION);
            var invocationsHashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, constructor);

            code.add(System.lineSeparator());
            code.add("// Initialize INVOCATIONS").add(System.lineSeparator());
            code.addStatement("$1T invocations = new $1T()", invocationsHashMap);

            for (var entry : systemInvocations.entrySet()) {
                code.addStatement("invocations.put($1T.class, $2T::new)", entry.getKey(), entry.getValue());
            }

            code.add(System.lineSeparator());
            code.addStatement("INVOCATIONS = $1T.unmodifiableMap(invocations)", Collections.class);

            return code.build();
        }

        private static MethodSpec getSystemMetadata(List<SystemJavaType> systems) {
            var returnType = ClassName.get("", "SystemMetadata");

            return MethodSpec.methodBuilder("getSystemMetadata")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.CLASS_WILDCARD, "clazz")
                    .returns(returnType)
                    .addStatement("return SYSTEMS.get(clazz)")
                    .build();
        }

        private static MethodSpec getUtilities(List<SystemJavaType> utilities) {
            // TODO eager utility instantiation might create unused EntityArchetypes for @Archetype methods

            var returnType = ParameterizedTypeName.get(ClassName.get(Map.class), Utils.CLASS_WILDCARD, ClassName.OBJECT);
            var hashMap = ParameterizedTypeName.get(ClassName.get(HashMap.class), Utils.CLASS_WILDCARD, ClassName.OBJECT);

            var code = CodeBlock.builder();
            code.addStatement("$1T world = ($1T) arg", Utils.INTERNAL_WORLD);

            code.add(System.lineSeparator());
            code.addStatement("$1T result = new $1T()", hashMap);

            for (var utility : utilities) {
                code.addStatement("result.put($1T.class, new $2T(world))", utility.source().className(), utility.className());
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

        private static MethodSpec createArchetypeEntityData() {
            var components = ParameterizedTypeName.get(ClassName.get(List.class), Utils.CLASS_WILDCARD);

            return MethodSpec.methodBuilder("createArchetypeEntityData")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(TypeName.INT, "entityBagSize")
                    .addParameter(components, "components")
                    .returns(Object.class)
                    .addStatement("return $1T.create(entityBagSize, components)", Utils.ENTITY_ARCHETYPE_DATA_IMPL)
                    .build();
        }

        private static MethodSpec getSystemInvocation() {
            var code = CodeBlock.builder();
            code.addStatement("$1T world = ($1T) arg", Utils.INTERNAL_WORLD);
            code.add(System.lineSeparator());

            var constructor = ParameterizedTypeName.get(ClassName.get(Function.class), Utils.INTERNAL_WORLD, Utils.SYSTEM_INVOCATION);
            code.addStatement("$1T constructor = INVOCATIONS.get(clazz)", constructor);
            code.beginControlFlow("if (constructor == null)");
            code.addStatement("throw new $1T(clazz)", Utils.INVALID_SYSTEM_INVOCATION_EXCEPTION);
            code.endControlFlow();

            code.add(System.lineSeparator());
            code.addStatement("return constructor.apply(world)");

            return MethodSpec.methodBuilder("getSystemInvocation")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Utils.CLASS_WILDCARD, "clazz")
                    .addParameter(Object.class, "arg")
                    .returns(Object.class)
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec set() {
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

}
