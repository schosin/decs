package de.schosin.decs.codegen.system.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
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
import de.schosin.decs.codegen.system.methods.UtilityMethod.TransmuteMethod;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;
import de.schosin.decs.codegen.utils.Utils;

public class TransmuteGenerator extends AbstractUtilityGenerator {

    public TransmuteGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator, "Transmute");
    }

    public Stream<TypeData> detect() {
        return roundEnv.getElementsAnnotatedWith(annotations.transmute()).stream()
                .map(ExecutableElement.class::cast)
                .map(this::handleTransmute);
    }

    @SuppressWarnings("unchecked")
    private UtilityData handleTransmute(ExecutableElement method) {
        var system = getUtilityType(method, "Transmute");
        if (system == null) {
            return null;
        }

        var error = false;

        if (!method.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @Transmute must not have type parameters.", method);
            return null;
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            printError("Methods annotated with @Transmute must return void.", method);
            error = true;
        }

        if (method.getModifiers().contains(Modifier.DEFAULT)) {
            printError("Methods annotated with @Transmute must not have a default implementation.", method);
            error = true;
        }

        if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
            printError("Methods annotated with @Transmute must be abstract.", method);
            error = true;
        }

        var methodParams = method.getParameters();
        var entityParam = methodParams.isEmpty() ? null : methodParams.getFirst();
        if (entityParam == null || !types.isAssignable(entityParam.asType(), elements.getTypeElement(Utils.BASE_ENTITY.canonicalName()).asType()) || !"entity".equals(entityParam.getSimpleName().toString())) {
            printError("@Transmute methods must start with \"Entity entity\", \"EntityRef entity\", or \"BaseEntity entity\".", method);
            error = true;
        }

        if (error) {
            return null;
        }

        // Resolve initializer method
        var initializer = resolveInitializerMethod(system, method);
        if (initializer == null) {
            // TODO initializer should be optional if only remove is present
            // TODO initializer should be optional if only enums are added
            return null;
        }

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

        // Resolve removed components
        var transmute = method.getAnnotationMirrors().stream()
                .filter(annotation -> annotation.getAnnotationType().equals(annotations.transmuteType()))
                .findFirst()
                .orElseThrow();

        var value = transmute.getElementValues().isEmpty() ? List.<AnnotationValue>of() : (List<AnnotationValue>) transmute.getElementValues().values().iterator().next().getValue();
        var removeTypes = value.stream()
                .map(AnnotationValue::getValue)
                .map(DeclaredType.class::cast)
                .map(ClassName::get)
                .toList();

        // Validate atleast one component added or removed
        if (enumParameters.isEmpty() && components.isEmpty() && removeTypes.isEmpty()) {
            printError("@Transmute must atleast add (@EntityInitializer parameter) or remove (@Transmute(remove = {...}) a component.", method);
            return null;
        }

        // Validate no overlap between added and removed components
        for (var component : components) {
            if (removeTypes.contains(component.type())) {
                printError("Component type '%s' cannot be used as a parameter (add) and in @Transmute (remove). Remove either the parameter or the annotation value."
                        .formatted(component.type()), component.parameter());

                error = true;
            }
        }

        if (error) {
            return null;
        }

        // TODO mismatch in enumComponents does not trigger an error, e.g. missing Moving in this example
        // protected abstract void createWhateverThisCouldBeCalled(int count, Player player, Position playerPos, Moving moving);
        // protected final void createWhateverThisCouldBeCalled(int index, Player player, Position playerPos, Position pos, Render render, WhateverThisCouldBeCalled wtcbc)

        var enumComponents = enumParameters.stream().map(parameter -> resolveParameter(method, parameter)).map(ComponentParameter.class::cast).toList();
        var transmuteMethod = new TransmuteMethod(method, initializer, method.getSimpleName().toString(), initializer.getSimpleName().toString(), (ClassName) ClassName.get(entityParam.asType()), parameters, enumComponents, components, removeTypes);
        return new UtilityData(system, transmuteMethod);
    }

    private List<Parameter> resolveParameters(ExecutableElement method, ExecutableElement initializer, List<? extends VariableElement> enumParameters) {
        var parameters = method.getParameters();
        var initParameters = initializer.getParameters();

        var size = parameters.size();

        var entityIdParam = initParameters.isEmpty() ? null : initParameters.getFirst();
        if (entityIdParam == null || entityIdParam.asType().getKind() != TypeKind.INT || !"entityId".equals(entityIdParam.getSimpleName().toString())) {
            printError("@EntityInitializer methods for @Transmute must start with \"int entityId\".", initializer);
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
                printError("@EntityInitializer parameter does not match @Transmute parameter '%s %s'. Use the same type and name for both."
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

    public GeneratorResult generate(TypeData typeData, List<TransmuteMethod> methods, Map<String, Integer> names) {
        var systemImpl = ClassName.get("", typeData.className().simpleName() + "Impl");

        var result = new GeneratorResult();

        for (var method : methods) {
            var originalName = method.methodName() + "Transmuter";

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
            result.fields().add(createField(method, type, fieldName));
            result.methods().add(createImplementation(method, type, fieldName));
            result.types().add(TypeGenerator.createType(systemImpl, method, type));

            // Offer archetype
            var code = result.offerArchetypeInit();
            code.addStatement("this.%s.set(archetype.id(), new $1T(this, archetype))".formatted(fieldName), type);
        }

        return result;
    }

    private FieldSpec createField(TransmuteMethod method, ClassName type, String fieldName) {
        var bag = ParameterizedTypeName.get(Utils.BAG, type);
        return FieldSpec.builder(bag, fieldName, Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $1T<>($2T.class, 8)", Utils.BAG, type)
                .build();
    }

    private MethodSpec createImplementation(TransmuteMethod method, ClassName type, String fieldName) {
        var parameters = method.parameters().stream()
                .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                .toList();
        var enumComponents = method.enumComponents().stream()
                .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                .toList();

        var code = CodeBlock.builder();
        code.addStatement("$1T _entity = ($1T) __entity", Utils.INTERNAL_BASE_ENTITY);
        code.addStatement("int _index = _entity.index()");
        code.addStatement("$1T _archetype = _entity.archetype()", Utils.ENTITY_ARCHETYPE);

        code.add(System.lineSeparator());
        code.add("// Retrieve target archetype, return early if null (entity marked for deletion)").add(System.lineSeparator());
        code.addStatement("$1T _transition = _archetype.moveEntity(_index, $2T.TRANSMUTATION)", Utils.TRANSITION, type);

        code.beginControlFlow("if (_transition == null)");
        code.addStatement("return");
        code.endControlFlow();

        // TODO no need for below code and for type if remove only

        code.add(System.lineSeparator());
        code.add("// Apply transmuter").add(System.lineSeparator());
        code.addStatement("$1T _transmuter = this.%s.get(_transition.archetype().id())".formatted(fieldName), type);
        code.add("_transmuter.apply(_transition.index(), _entity.id()");
        for (var parameter : parameters) {
            code.add(", ").add(parameter.name());
        }
        for (var component : enumComponents) {
            code.add(", ").add(component.name());
        }
        code.addStatement(")");

        code.add(System.lineSeparator());
        code.addStatement("_transition.free()");

        return MethodSpec.methodBuilder(method.methodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(method.entityParam(), "__entity")
                .addParameters(parameters)
                .addParameters(enumComponents)
                .addCode(code.build())
                .build();
    }

    private static class TypeGenerator {

        private static TypeSpec createType(ClassName systemImpl, TransmuteMethod method, ClassName type) {
            return TypeSpec.classBuilder(type)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addField(transmutation(method))
                    .addField(systemImpl, "_system", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(componentFields(method))
                    .addFields(enumComponentFields(method))
                    .addMethod(constructor(method, systemImpl))
                    .addMethod(apply(method))
                    .build();
        }

        private static FieldSpec transmutation(TransmuteMethod method) {
            var code = CodeBlock.builder();
            code.add("$1T.builder()", Utils.TRANSMUTATION);
            code.indent();

            var comma = false;

            var components = method.components();
            if (!components.isEmpty()) {
                code.add(System.lineSeparator()).add(".add(");

                for (var component : components) {
                    if (comma) {
                        code.add(", ");
                    }
                    comma = true;

                    code.add("$1T.class", component.type());
                }

                code.add(")");
            }

            var enumComponents = method.enumComponents();
            if (!enumComponents.isEmpty()) {
                code.add(System.lineSeparator()).add(".add(");

                for (var component : enumComponents) {
                    if (comma) {
                        code.add(", ");
                    }
                    comma = true;

                    code.add("$1T.class", component.type());
                }

                code.add(")");
            }

            var remove = method.remove();
            if (!remove.isEmpty()) {
                code.add(System.lineSeparator()).add(".remove(");

                for (int i = 0, s = remove.size(); i < s; i++) {
                    var component = remove.get(i);

                    if (i > 0) {
                        code.add(", ");
                    }

                    code.add("$1T.class", component);
                }

                code.add(")");
            }

            code.add(System.lineSeparator()).add(".build()");
            code.unindent();

            return FieldSpec.builder(Utils.TRANSMUTATION, "TRANSMUTATION", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(code.build())
                    .build();
        }

        private static List<FieldSpec> componentFields(TransmuteMethod method) {
            return method.components().stream()
                    .map(component -> FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, component.type()), component.fieldName(), Modifier.PRIVATE, Modifier.FINAL).build())
                    .toList();
        }

        private static List<FieldSpec> enumComponentFields(TransmuteMethod method) {
            return method.enumComponents().stream()
                    .filter(component -> switch (component.data()) {
                        case ComponentData.EnumComponent c -> true;
                        case ComponentData.ClassComponent c -> false;
                        case ComponentData.SingletonEnumComponent c -> false;
                        case ComponentData.InterfaceComponent c -> false;
                    })
                    .map(component -> FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, component.type()), component.fieldName(), Modifier.PRIVATE, Modifier.FINAL).build())
                    .toList();
        }

        private static MethodSpec constructor(TransmuteMethod method, ClassName systemImpl) {
            var code = CodeBlock.builder();
            code.addStatement("this._system = system");

            code.add(System.lineSeparator());
            code.addStatement("$1T _data = archetype.getData()", Utils.ENTITY_ARCHETYPE_DATA_IMPL);

            for (var component : method.components()) {
                code.addStatement("this.%1$s = _data.%1$s".formatted(component.fieldName()));
            }
            for (var component : method.enumComponents()) {
                switch (component.data()) {
                    case ComponentData.EnumComponent c -> {
                        code.addStatement("this.%1$s = _data.%1$s".formatted(component.fieldName()));
                    }
                    case ComponentData.ClassComponent c -> {
                    }
                    case ComponentData.SingletonEnumComponent c -> {
                    }
                    case ComponentData.InterfaceComponent c -> {
                        code.add("// TOOD initialize interface component field: %s".formatted(c.className().simpleName())).add(System.lineSeparator());
                    }
                }
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(systemImpl, "system")
                    .addParameter(Utils.ENTITY_ARCHETYPE, "archetype")
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec apply(TransmuteMethod method) {
            var parameters = method.parameters().stream()
                    .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                    .toList();
            var enumComponents = method.enumComponents().stream()
                    .map(param -> ParameterSpec.builder(param.type(), param.name()).build())
                    .toList();

            var code = CodeBlock.builder();

            if (!method.enumComponents().isEmpty()) {
                for (var component : method.enumComponents()) {
                    switch (component.data()) {
                        case ComponentData.EnumComponent c ->
                                code.addStatement("this.%s.setUnsafe(_index, %s)".formatted(component.fieldName(), component.name()));
                        case ComponentData.ClassComponent c -> {
                        }
                        case ComponentData.SingletonEnumComponent c -> {
                        }
                        case ComponentData.InterfaceComponent c -> {
                        }
                    }
                }

                code.add(System.lineSeparator());
            }

            code.add("this._system.%s(_entityId".formatted(method.initializerName()));
            for (var parameter : method.parameters()) {
                code.add(", %s".formatted(parameter.name()));
            }
            for (var component : method.enumComponents()) {
                code.add(", %s".formatted(component.name()));
            }
            for (var component : method.components()) {
                code.add(", this.%s.getUnsafe(_index)".formatted(component.fieldName()));
            }
            code.addStatement(")");

            return MethodSpec.methodBuilder("apply")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .addParameter(TypeName.INT, "_index")
                    .addParameter(TypeName.INT, "_entityId")
                    .addParameters(parameters)
                    .addParameters(enumComponents)
                    .addCode(code.build())
                    .build();
        }

    }

}
