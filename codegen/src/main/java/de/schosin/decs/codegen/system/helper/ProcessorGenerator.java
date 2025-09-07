package de.schosin.decs.codegen.system.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.TypeData.SystemData;
import de.schosin.decs.codegen.system.methods.ProcessorMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod.EntityProcessorMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod.SystemProcessorMethod;
import de.schosin.decs.codegen.utils.ParameterData;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;
import de.schosin.decs.codegen.utils.ParameterData.EntityIdParameter;
import de.schosin.decs.codegen.utils.ParameterData.EntityParameter;
import de.schosin.decs.codegen.utils.ParameterData.FieldProvider;
import de.schosin.decs.codegen.utils.ParameterData.InvalidParameter;
import de.schosin.decs.codegen.utils.ParameterData.LocalVariableProvider;
import de.schosin.decs.codegen.utils.ParameterData.SystemParameterData;
import de.schosin.decs.codegen.utils.Utils;

public class ProcessorGenerator extends AbstractSystemGenerator {

    public record ProcessorResult(List<FieldSpec> fields, List<TypeSpec> types, CodeBlock.Builder fieldInit,
            CodeBlock.Builder runImpl) {

        private static final ProcessorResult EMPTY = new ProcessorResult(List.of(), List.of(), CodeBlock.builder(), CodeBlock.builder());

        public ProcessorResult() {
            this(new ArrayList<>(), new ArrayList<>(), CodeBlock.builder(), CodeBlock.builder());
        }

    }

    public ProcessorGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator);
    }

    public Stream<SystemData> detect() {
        return Stream.concat(detectSystemProcessor(), detectEntityProcessor());
    }

    private Stream<SystemData> detectSystemProcessor() {
        return roundEnv.getElementsAnnotatedWith(annotations.systemProcessor()).stream()
                .map(ExecutableElement.class::cast)
                .map(this::handleSystemProcessor);
    }

    private Stream<SystemData> detectEntityProcessor() {
        return roundEnv.getElementsAnnotatedWith(annotations.entityProcessor()).stream()
                .map(ExecutableElement.class::cast)
                .map(this::handleEntityProcessor);
    }

    private SystemData handleSystemProcessor(ExecutableElement method) {
        var system = getSystemType(method, "SystemProcessor");
        if (system == null) {
            return null;
        }

        if (!method.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @SystemProcessor must not have type parameters.", method);
            return null;
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            printError("Methods annotated with @SystemProcessor must return void.", method);
            return null;
        }

        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            printError("Methods annotated with @SystemProcessor must not be private.", method);
            return null;
        }

        if (!method.getModifiers().contains(Modifier.FINAL)) {
            printError("Methods annotated with @SystemProcessor must be final.", method);
            return null;
        }

        var parameters = resolveSystemParameters(method, "SystemProcessor");
        var methodData = new SystemProcessorMethod(method, method.getSimpleName().toString(), parameters);

        return new SystemData(system, CompositionData.detect(system, generator), methodData);
    }

    private List<SystemParameterData> resolveSystemParameters(ExecutableElement method, String annotation) {
        var parameters = resolveParameters(method);

        var error = false;
        for (var parameter : parameters) {
            error |= switch (parameter) {
                case SystemParameterData p -> false;
                case ComponentParameter p -> printError("Cannot use a component type as a system parameter.", parameter.parameter());
                case EntityIdParameter p -> printError("Cannot use non-@Value int as a system parameter.", parameter.parameter());
                case EntityParameter p -> printError("Cannot use Entity as a system parameter.", parameter.parameter());
                case InvalidParameter p -> true;
            };
        }

        if (error) {
            return null;
        }

        return parameters.stream().map(SystemParameterData.class::cast).toList();
    }

    private SystemData handleEntityProcessor(ExecutableElement method) {
        var system = getSystemType(method, "EntityProcessor");
        if (system == null) {
            return null;
        }

        if (!method.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @SystemProcessor must not have type parameters.", method);
            return null;
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            printError("Methods annotated with @EntityProcessor must return void.", method);
            return null;
        }

        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            printError("Methods annotated with @EntityProcessor must not be private.", method);
            return null;
        }

        if (!method.getModifiers().contains(Modifier.FINAL)) {
            printError("Methods annotated with @EntityProcessor must be final.", method);
            return null;
        }

        var parameters = resolveProcessorParameters(method, "EntityProcessor");

        var systemComposition = CompositionData.detect(system, generator);
        var composition = CompositionData.detect(method, generator);
        if (!validateComposition(method, parameters, composition != null ? composition : systemComposition, "EntityProcessor")) {
            return null;
        }

        var methodData = new EntityProcessorMethod(method, method.getSimpleName().toString(), composition, parameters);
        return new SystemData(system, systemComposition, methodData);
    }

    public ProcessorResult generate(ClassName className, SystemData system, HashMap<String, Integer> names) {
        if (system.methods().isEmpty()) {
            return ProcessorResult.EMPTY;
        }

        var result = new ProcessorResult();
        EntityProcessorMethod previousModifying = null;

        var methods = system.methods();
        for (int i = 0, s = methods.size(); i < s; i++) {
            var method = methods.get(i);

            switch (method) {
                case SystemProcessorMethod processor -> processSystemMethod(className, system, processor, names, result);
                case EntityProcessorMethod processor -> processEntityMethod(className, system, processor, names, result, previousModifying);
            }

            if (method instanceof EntityProcessorMethod processor) {
                previousModifying = system.modifying() || processor.modifying() ? processor : null;
            }

            if (i < s - 1) {
                result.runImpl.add(System.lineSeparator());
            }
        }

        return result;
    }

    private void processSystemMethod(ClassName className, SystemData system, SystemProcessorMethod method, HashMap<String, Integer> names, ProcessorResult result) {
        var code = result.runImpl;
        var parameters = method.parameters();

        code.add("// %s%s".formatted(formatMethod(method), System.lineSeparator()));

        code.add("%s(".formatted(method.methodName()));
        for (int i = 0, s = parameters.size(); i < s; i++) {
            var parameter = parameters.get(i);

            if (i > 0) {
                code.add(", ");
            }

            parameter.systemAccessor(code);
        }
        code.addStatement(")");
    }

    private void processEntityMethod(ClassName className, SystemData system, EntityProcessorMethod method, HashMap<String, Integer> names, ProcessorResult result,
            EntityProcessorMethod previousModifying) {
        // Flush changes if a previous
        if (previousModifying != null) {
            // TODO two systems run in parallel, one flushes while the other writes to an archetype being flushed
            // TODO flushing an archetype must block modifications -> writeLock

            result.runImpl.add("// Flush potential entity changes by \"this.%s\"".formatted(previousModifying.methodName())).add(System.lineSeparator());
            result.runImpl.addStatement("this._world.flushChanges()");
            result.runImpl.add(System.lineSeparator());
        }

        var originalName = method.methodName();

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
        result.fields.add(createField(type, fieldName));
        result.types.add(TypeGenerator.createType(className, system, method, type));

        createRunImplementation(method, fieldName, type, result.runImpl);
        result.fieldInit.addStatement("this.%s = new $1T<>($2T.class, 4)".formatted(fieldName), Utils.BAG, type);
    }

    private FieldSpec createField(ClassName type, String fieldName) {
        var bag = ParameterizedTypeName.get(Utils.BAG, type);

        return FieldSpec.builder(bag, fieldName, Modifier.PRIVATE, Modifier.FINAL).build();
    }

    private void createRunImplementation(EntityProcessorMethod method, String fieldName, ClassName type, CodeBlock.Builder code) {
        code.add("// %s%s".formatted(formatMethod(method), System.lineSeparator()));
        code.addStatement("$1T[] %1$sData = this.%1$s.getData()".formatted(fieldName), type);

        code.beginControlFlow("for (int i = 0, s = this.%s.size(); i<s; i++)".formatted(fieldName));
        code.addStatement("%sData[i].run()".formatted(fieldName));
        code.endControlFlow();
    }

    private String formatMethod(ProcessorMethod method) {
        var result = new StringBuilder();

        result.append(method.methodName()).append("(");

        var parameters = method.parameters();
        for (int i = 0, s = parameters.size(); i < s; i++) {
            var parameter = parameters.get(i);

            if (i > 0) {
                result.append(", ");
            }

            result.append(parameter.type());
        }

        result.append(")");

        return result.toString();
    }

    static class TypeGenerator {

        public static TypeSpec createType(ClassName className, SystemData system, EntityProcessorMethod method, ClassName type) {
            var composition = method.composition() != null ? method.composition() : system.composition();
            var parameters = method.parameters();

            var fields = createFields(parameters, type);
            var componentFields = createComponentFields(parameters, composition, type);
            var entityParameter = parameters.stream().anyMatch(EntityParameter.class::isInstance);

            var spec = TypeSpec.classBuilder(type)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addField(Utils.buildComposition(composition))
                    .addField(Utils.WORLD, "_world", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.ENTITY_ARCHETYPE, "_archetype", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(className, "_system", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(fields)
                    .addFields(componentFields)
                    .addMethod(constructor(className, parameters, composition, entityParameter))
                    .addMethod(run(className, method, composition, entityParameter));

            if (entityParameter) {
                spec.addField(FieldSpec.builder(Utils.INTERNAL_ENTITY, "_entity", Modifier.PRIVATE, Modifier.FINAL).build());
            }

            return spec.build();
        }

        static List<FieldSpec> createFields(List<ParameterData> parameters, ClassName processor) {
            return parameters.stream()
                    .filter(FieldProvider.class::isInstance)
                    .map(FieldProvider.class::cast)
                    .map(FieldProvider::fieldSpec)
                    .distinct()
                    .toList();
        }

        static List<FieldSpec> createComponentFields(List<ParameterData> parameters, CompositionData composition, ClassName processor) {
            return parameters.stream()
                    .filter(ComponentParameter.class::isInstance)
                    .map(ComponentParameter.class::cast)
                    .map(component -> component.fieldSpec(composition))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        static MethodSpec constructor(ClassName className, List<ParameterData> parameters, CompositionData composition, boolean entityParameter) {
            var code = CodeBlock.builder();
            code.addStatement("this._world = world");
            code.addStatement("this._archetype = archetype");
            code.addStatement("this._system = system");

            var fieldProviders = parameters.stream()
                    .filter(FieldProvider.class::isInstance)
                    .map(FieldProvider.class::cast)
                    .filter(Utils.distinctBy(FieldProvider::fieldSpec))
                    .toList();

            if (!fieldProviders.isEmpty()) {
                code.add(System.lineSeparator());

                for (var fieldProvider : fieldProviders) {
                    fieldProvider.fieldInit(code, "world", "archetype");
                }
            }

            var components = parameters.stream()
                    .filter(ComponentParameter.class::isInstance)
                    .map(ComponentParameter.class::cast)
                    .toList();
            if (!components.isEmpty()) {
                code.add(System.lineSeparator());

                for (var component : components) {
                    component.fieldInit(code, "world", "archetype", composition);
                }
            }

            if (entityParameter) {
                code.add(System.lineSeparator());
                code.addStatement("this._entity = new $1T(archetype)", Utils.INTERNAL_ENTITY);
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(Utils.WORLD, "world")
                    .addParameter(Utils.ENTITY_ARCHETYPE, "archetype")
                    .addParameter(className, "system")
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec run(ClassName className, EntityProcessorMethod method, CompositionData composition, boolean entityParameter) {
            var code = CodeBlock.builder();
            var parameters = method.parameters();

            // Check size
            code.add("// Return early if no entities").add(System.lineSeparator());
            code.addStatement("int _s = this._archetype.getAlive()");
            code.beginControlFlow("if (_s == 0)");
            code.addStatement("return");
            code.endControlFlow();
            code.add(System.lineSeparator());

            // Prepare local variables
            var hasLocalVars = false;
            for (var parameter : parameters) {
                if (parameter instanceof LocalVariableProvider localVar) {
                    localVar.localVariable(code, composition);
                    hasLocalVars = true;
                }
            }

            if (hasLocalVars) {
                code.add(System.lineSeparator());
            }

            code.addStatement("int[] _data = this._archetype.getEntities().getData()");
            if (entityParameter) {
                code.addStatement("$1T _entity = this._entity", Utils.INTERNAL_ENTITY);
            }

            // Iterate archetype entities
            code.add(System.lineSeparator());
            code.add("// Iterate entities").add(System.lineSeparator());
            code.beginControlFlow("for (int _i = 0; _i < _s; _i++)");

            if (entityParameter) {
                code.addStatement("_entity.index = _i");
                code.addStatement("_entity.entityId = _data[_i]");
                code.add(System.lineSeparator());
            }

            code.add("this._system.%s(".formatted(method.methodName()));
            for (int i = 0, s = parameters.size(); i < s; i++) {
                var parameter = parameters.get(i);

                if (i > 0) {
                    code.add(", ");
                }

                parameter.entityAccessor(code, "_data", "_i", composition);
            }
            code.addStatement(")");

            code.endControlFlow();

            if (entityParameter) {
                // Reset entity fields to -1 to cause errors on improper use outside of the main loop
                code.add(System.lineSeparator());
                code.add("// Reset state to avoid improper use as Entity outside of the loop").add(System.lineSeparator());
                code.addStatement("_entity.index = -1");
                code.addStatement("_entity.entityId = -1");
            }

            return MethodSpec.methodBuilder("run")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .addCode(code.build())
                    .build();
        }

    }

}
