package de.schosin.decs.codegen.system.helper;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.TypeData.UtilityData;
import de.schosin.decs.codegen.system.methods.UtilityMethod.CountMethod;
import de.schosin.decs.codegen.utils.Utils;

public class CountGenerator extends AbstractUtilityGenerator {

    public CountGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator, "Count");
    }

    public Stream<UtilityData> detect() {
        return roundEnv.getElementsAnnotatedWith(annotations.count()).stream()
                .map(ExecutableElement.class::cast)
                .map(this::handleCount);
    }

    private UtilityData handleCount(ExecutableElement method) {
        var system = getUtilityType(method, "Count");
        if (system == null) {
            return null;
        }

        var error = false;

        if (!method.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @Count must not have type parameters.", method);
            return null;
        }

        if (!method.getParameters().isEmpty()) {
            printError("Methods annotated with @Count must have no parameters.", method);
            error = true;
        }

        if (method.getReturnType().getKind() != TypeKind.INT) {
            printError("Methods annotated with @Count must return 'int'.", method);
            error = true;
        }

        if (method.getModifiers().contains(Modifier.DEFAULT)) {
            printError("Methods annotated with @Count must not have a default implementation.", method);
            error = true;
        }

        if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
            printError("Methods annotated with @Count must be abstract.", method);
            error = true;
        }

        if (error) {
            return null;
        }

        var systemComposition = CompositionData.detect(system, generator);
        var composition = CompositionData.detect(method, generator);
        if (!validateComposition(method, List.of(), composition != null ? composition : systemComposition, "Count")) {
            return null;
        }

        return new UtilityData(system, new CountMethod(method, method.getSimpleName().toString(), composition != null ? composition : systemComposition));
    }

    public GeneratorResult generate(List<CountMethod> methods, Map<String, Integer> names) {
        var result = new GeneratorResult();

        for (var method : methods) {
            var originalName = method.methodName() + "CountArchetypes";

            // Resolve final name in case of overloads
            var fieldName = originalName;
            int count = names.merge(fieldName, 1, Integer::sum);

            while (count > 1) {
                fieldName = originalName + count;
                count = names.merge(fieldName, 1, Integer::sum);
            }

            var constantName = Utils.toUpperSnakeCase(fieldName);

            // Generate code
            result.fields().add(Utils.buildComposition(method.composition(), constantName));
            result.fields().add(createArchetypesField(fieldName));
            result.methods().add(createImplementation(method, fieldName));

            var offerArchetype = result.offerArchetypeInit();
            offerArchetype.beginControlFlow("if (%s.matches(archetype))".formatted(constantName));
            offerArchetype.addStatement("this.%s.add(archetype)".formatted(fieldName));
            offerArchetype.endControlFlow();
        }

        return result;
    }

    private FieldSpec createArchetypesField(String fieldName) {
        var type = ParameterizedTypeName.get(Utils.BAG, Utils.ENTITY_ARCHETYPE);

        return FieldSpec.builder(type, fieldName, Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $1T<>($2T.class, 8)", Utils.BAG, Utils.ENTITY_ARCHETYPE)
                .build();
    }

    private MethodSpec createImplementation(CountMethod method, String fieldName) {
        var code = CodeBlock.builder();

        code.addStatement("int count = 0");

        code.add(System.lineSeparator());
        code.addStatement("$1T[] data = this.%s.getData()".formatted(fieldName), Utils.ENTITY_ARCHETYPE);
        code.beginControlFlow("for (int i = 0, s = this.%s.size(); i < s; i++)".formatted(fieldName));
        code.addStatement("count += data[i].getAlive()");
        code.endControlFlow();

        code.add(System.lineSeparator());
        code.addStatement("return count");

        return MethodSpec.methodBuilder(method.methodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(ParameterizedTypeName.INT)
                .addCode(code.build())
                .build();
    }

}
