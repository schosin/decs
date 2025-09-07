package de.schosin.decs.codegen.system.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import de.schosin.decs.codegen.components.ComponentData;
import de.schosin.decs.codegen.components.ComponentData.EnumComponentData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.TypeData;
import de.schosin.decs.codegen.system.TypeData.UtilityData;
import de.schosin.decs.codegen.system.methods.UtilityMethod.ArchetypeMethod;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;
import de.schosin.decs.codegen.utils.Utils;

public class ArchetypeGenerator extends AbstractUtilityGenerator {

    public ArchetypeGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator, "Archetype");
    }

    public Stream<UtilityData> detect() {
        return roundEnv.getElementsAnnotatedWith(annotations.archetype()).stream()
                .map(ExecutableElement.class::cast)
                .map(this::handleArchetype);
    }

    private UtilityData handleArchetype(ExecutableElement method) {
        var system = getUtilityType(method, "Archetype");
        if (system == null) {
            return null;
        }

        var error = false;

        if (!method.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @Archetype must not have type parameters.", method);
            return null;
        }

        var returnType = method.getReturnType();
        var returnsEntityRefs = Utils.ENTITY_BAG.canonicalName().equals(returnType.toString());
        if (!returnsEntityRefs && returnType.getKind() != TypeKind.VOID) {
            printError("Methods annotated with @Archetype must return void or %s.".formatted(Utils.ENTITY_BAG.simpleName()), method);
            error = true;
        }

        if (method.getModifiers().contains(Modifier.DEFAULT)) {
            printError("Methods annotated with @Archetype must not have a default implementation.", method);
            error = true;
        }

        if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
            printError("Methods annotated with @Archetype must be abstract.", method);
            error = true;
        }

        var methodParams = method.getParameters();
        var countParam = methodParams.isEmpty() ? null : methodParams.getFirst();
        if (countParam == null || countParam.asType().getKind() != TypeKind.INT || !"count".equals(countParam.getSimpleName().toString())) {
            printError("@Archetype methods must start with \"int count\".", method);
            error = true;
        }

        if (error) {
            return null;
        }

        // Resolve initializer method
        var initializer = resolveInitializerMethod(system, method);
        if (initializer == null) {
            return null;
        }

        // TODO initializer could be optional if only enums are added (however much sense that makes)

        // Resolve trailing enum parameters
        var params = method.getParameters();
        var enumParameters = IntStream.range(0, params.size() - 1)
                .map(i -> params.size() - i - 1)
                .mapToObj(params::get)
                .takeWhile(parameter -> components.getWrittenComponent(parameter.asType()) instanceof EnumComponentData)
                .collect(Collectors.toList())
                .reversed();

        // Resolve parameters (common prefix)
        var parameters = resolveParameters(method, initializer, enumParameters);
        if (parameters == null) {
            return null;
        }

        // Resolve components
        var components = resolveComponents(method, initializer, parameters, enumParameters);
        if (components == null) {
            return null;
        }

        // Validate atleast one component addded
        if (enumParameters.isEmpty() && components.isEmpty()) {
            printError("@Archetype must add atleast one component (@EntityInitializer parameter).", initializer);
            return null;
        }

        // TODO mismatch in enumComponents does not trigger an error

        var enumComponents = enumParameters.stream().map(parameter -> resolveParameter(method, parameter)).map(ComponentParameter.class::cast).toList();
        return new UtilityData(system, new ArchetypeMethod(method, initializer, method.getSimpleName().toString(), initializer.getSimpleName().toString(), returnsEntityRefs, parameters, enumComponents, components));
    }

    private List<Parameter> resolveParameters(ExecutableElement method, ExecutableElement initializer, List<? extends VariableElement> enumParameters) {
        var parameters = method.getParameters();
        var initParameters = initializer.getParameters();

        var size = parameters.size();

        var indexParam = initParameters.isEmpty() ? null : initParameters.getFirst();
        if (indexParam == null || indexParam.asType().getKind() != TypeKind.INT || !"index".equals(indexParam.getSimpleName().toString())) {
            printError("@EntityInitializer methods for @Archetype must start with \"int index\".", initializer);
            return null;
        }

        var error = false;

        if (size > initParameters.size()) {
            for (int i = initParameters.size(); i < size; i++) {
                var parameter = parameters.get(i);

                printError("Parameter must also occur on @EntityInitializer method in the same position.", parameter);
                error = true;
            }
        }

        // TODO validate enums match

        var result = new ArrayList<Parameter>(size);
        for (int i = 1; i < size - enumParameters.size(); i++) {
            var parameter = parameters.get(i);
            var initParameter = initParameters.get(i);

            var typeMismatch = !parameter.asType().equals(initParameter.asType());
            var nameMismatch = !parameter.getSimpleName().toString().equals(initParameter.getSimpleName().toString());

            if (typeMismatch || nameMismatch) {
                printError("@EntityInitializer parameter does not match @Archetype parameter '%s %s'. Use the same type and name for both."
                        .formatted(parameter.asType(), parameter.getSimpleName()), initParameter);

                error = true;
                continue;
            }

            result.add(new Parameter(TypeName.get(parameter.asType()), parameter.getSimpleName().toString()));
        }

        if (error) {
            return null;
        }

        return result;
    }

    public GeneratorResult generate(TypeData typeData, List<ArchetypeMethod> methods, Map<String, Integer> names) {
        var className = ClassName.get("", typeData.className().simpleName() + "Impl");

        var result = new GeneratorResult();
        var code = result.fieldInit();

        for (var method : methods) {
            var originalName = method.methodName() + "Archetype";

            // Resolve final name in case of overloads
            var fieldName = originalName;
            int count = names.merge(fieldName, 1, Integer::sum);

            while (count > 1) {
                fieldName = originalName + count;
                count = names.merge(fieldName, 1, Integer::sum);
            }

            var typeName = Utils.capitalize(fieldName);
            var type = ClassName.get("", typeName);

            // Generate code
            if (method.returnsEntityRefs()) {
                result.requiresWorld().set(true);
            }

            result.fields().add(createField(type, fieldName));
            result.methods().add(createImplementation(method, fieldName));
            result.types().add(TypeGenerator.createType(className, method, type));

            code.add("this.%s = new $1T(world.getEntityArchetype(".formatted(fieldName), type);

            var comma = false;
            for (int a = 0, as = method.components().size(); a < as; a++) {
                var param = method.components().get(a);

                if (comma) {
                    code.add(", ");
                }
                comma = true;

                code.add("$1T.class", param.type());
            }
            for (int a = 0, as = method.enumComponents().size(); a < as; a++) {
                var param = method.enumComponents().get(a);

                if (comma) {
                    code.add(", ");
                }
                comma = true;

                code.add("$1T.class", param.type());
            }

            code.addStatement("))");
        }

        return result;
    }

    private FieldSpec createField(ClassName type, String fieldName) {
        return FieldSpec.builder(type, fieldName, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private MethodSpec createImplementation(ArchetypeMethod method, String fieldName) {
        var parameters = method.parameters().stream()
                .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                .toList();
        var enumComponents = method.enumComponents().stream()
                .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                .toList();

        var code = CodeBlock.builder();

        var returnType = TypeName.VOID;
        if (method.returnsEntityRefs()) {
            returnType = Utils.ENTITY_BAG;
            code.addStatement("$1T _result = this._world.createEntityBag()", Utils.ENTITY_BAG);
            code.add("this.%s.apply(_result, count".formatted(fieldName));
        } else {
            code.add("this.%s.apply(count".formatted(fieldName));
        }

        for (var parameter : parameters) {
            code.add(", ").add(parameter.name());
        }
        for (var component : enumComponents) {
            code.add(", ").add(component.name());
        }
        code.addStatement(")");

        if (method.returnsEntityRefs()) {
            code.add(System.lineSeparator());
            code.addStatement("return _result");
        }

        return MethodSpec.methodBuilder(method.methodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.INT, "count")
                .addParameters(parameters)
                .addParameters(enumComponents)
                .returns(returnType)
                .addCode(code.build())
                .build();
    }

    private static class TypeGenerator {

        private static TypeSpec createType(ClassName className, ArchetypeMethod method, ClassName type) {
            return TypeSpec.classBuilder(type)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.ENTITY_ARCHETYPE, "_archetype", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(componentFields(method))
                    .addFields(enumComponentFields(method))
                    .addMethod(constructor(method))
                    .addMethod(apply(className, method))
                    .build();
        }

        private static List<FieldSpec> componentFields(ArchetypeMethod method) {
            return method.components().stream()
                    .map(param -> FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, param.type()), param.fieldName(), Modifier.PRIVATE, Modifier.FINAL).build())
                    .toList();
        }

        private static List<FieldSpec> enumComponentFields(ArchetypeMethod method) {
            return method.enumComponents().stream()
                    .filter(component -> switch (component.data()) {
                        case ComponentData.EnumComponent c -> true;
                        case ComponentData.ClassComponent c -> false;
                        case ComponentData.SingletonEnumComponent c -> false;
                    })
                    .map(component -> FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, component.type()), component.fieldName(), Modifier.PRIVATE, Modifier.FINAL).build())
                    .toList();
        }

        private static MethodSpec constructor(ArchetypeMethod method) {
            var code = CodeBlock.builder();
            code.addStatement("this._archetype = archetype");

            for (var component : method.components()) {
                code.addStatement("this.%s = archetype.getData($1T.class)".formatted(component.fieldName()), component.type());
            }
            for (var component : method.enumComponents()) {
                switch (component.data()) {
                    case ComponentData.EnumComponent c ->
                            code.addStatement("this.%s = archetype.getData($1T.class)".formatted(component.fieldName()), component.type());
                    case ComponentData.ClassComponent c -> {
                    }
                    case ComponentData.SingletonEnumComponent c -> {
                    }
                }
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(Utils.ENTITY_ARCHETYPE, "archetype")
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec apply(ClassName className, ArchetypeMethod method) {
            // Parameters
            var parameters = new ArrayList<ParameterSpec>();
            if (method.returnsEntityRefs()) {
                parameters.add(ParameterSpec.builder(Utils.ENTITY_BAG, "_entityRefs").build());
            }

            parameters.add(ParameterSpec.builder(TypeName.INT, "count").build());
            method.parameters().stream()
                    .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                    .forEach(parameters::add);

            method.enumComponents().stream()
                    .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                    .forEach(parameters::add);

            // Code
            var code = CodeBlock.builder();
            code.addStatement("int _index = this._archetype.createEntities(count)");

            if (!method.components().isEmpty() || !method.enumComponents().isEmpty()) {
                code.add(System.lineSeparator());

                for (var component : method.components()) {
                    code.addStatement("$1T[] _%s = this.%s.getData()".formatted(component.name(), component.fieldName()), component.type());
                }
            }

            code.add(System.lineSeparator());
            code.beginControlFlow("for (int _idx = 0; _idx < count; _idx++)");

            code.addStatement("int _i = _index + _idx");
            if (method.returnsEntityRefs()) {
                code.addStatement("_entityRefs.add(_archetype.createEntityRef(_i))");
            }
            code.add(System.lineSeparator());

            if (!method.enumComponents().isEmpty()) {
                var added = false;
                for (var component : method.enumComponents()) {
                    switch (component.data()) {
                        case ComponentData.EnumComponent c -> {
                            code.addStatement("this.%s.set(_i, %s)".formatted(component.fieldName(), component.name()));
                            added = true;
                        }
                        case ComponentData.ClassComponent c -> {
                        }
                        case ComponentData.SingletonEnumComponent c -> {
                        }
                    }
                }

                if (added) {
                    code.add(System.lineSeparator());
                }
            }

            code.add("$1T.this.%s(_idx".formatted(method.initializerName()), className);
            for (var parameter : method.parameters()) {
                code.add(", %s".formatted(parameter.name()));
            }
            for (var component : method.enumComponents()) {
                code.add(", %s".formatted(component.name()));
            }
            for (var component : method.components()) {
                code.add(", _%s[_i]".formatted(component.name()));
            }
            code.addStatement(")");

            code.endControlFlow();

            return MethodSpec.methodBuilder("apply")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .addParameters(parameters)
                    .addCode(code.build())
                    .build();
        }

    }

}
