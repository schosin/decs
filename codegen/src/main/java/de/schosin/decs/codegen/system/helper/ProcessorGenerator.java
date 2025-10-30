package de.schosin.decs.codegen.system.helper;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentData.InterfaceComponent;
import de.schosin.decs.codegen.entityarchetype.EntityArchetypeDataGenerator;
import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.TypeData.SystemData;
import de.schosin.decs.codegen.system.methods.SystemMethod;
import de.schosin.decs.codegen.system.methods.ProcessorMethod;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.EntityProcessorMethod;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.EntityProcessorMethod.ParallelConfig;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.EntityProcessorMethod.ParallelStrategy;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.SystemProcessorMethod;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.ParameterData;
import de.schosin.decs.codegen.utils.ParameterData.*;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessorGenerator extends AbstractSystemGenerator {

    public record ProcessorResult(List<FieldSpec> fields, List<TypeSpec> types, CodeBlock.Builder fieldInit,
                                  CodeBlock.Builder runImpl) {

        private static final ProcessorResult EMPTY = new ProcessorResult(List.of(), List.of(), CodeBlock.builder(), CodeBlock.builder());

        public ProcessorResult() {
            this(new ArrayList<>(), new ArrayList<>(), CodeBlock.builder(), CodeBlock.builder());
        }

    }

    private static final FieldSpec EXECUTOR = FieldSpec.builder(ExecutorService.class, "_executor", Modifier.PRIVATE, Modifier.FINAL).build();

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

        var systemProcessor = resolveAnnotation(method, Utils.SYSTEM_PROCESSOR);
        var modifying = !systemProcessor.getElementValues().isEmpty() && Boolean.TRUE.equals(systemProcessor.getElementValues().values().iterator().next().getValue());

        var parameters = resolveSystemParameters(method, "SystemProcessor");
        var methodData = new SystemProcessorMethod(method, method.getSimpleName().toString(), modifying, parameters);

        return new SystemData(system, CompositionData.detect(system, generator), methodData);
    }

    private List<SystemParameterData> resolveSystemParameters(ExecutableElement method, String annotation) {
        var parameters = resolveParameters(method);

        var error = false;
        for (var parameter : parameters) {
            error |= switch (parameter) {
                case SystemParameterData p -> false;
                case ComponentParameter p ->
                        printError("Cannot use a component type as a system parameter.", parameter.parameter());
                case EntityIdParameter p ->
                        printError("Cannot use non-@Value int as a system parameter.", parameter.parameter());
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

        var parallel = resolveParallelStrategy(method);
        var inline = resolveAnnotation(method, Utils.INLINE);

        var hasInterfaceComponents = parameters.stream().anyMatch(parameter -> parameter instanceof ComponentParameter component && component.data() instanceof InterfaceComponent);
        if (inline != null && !hasInterfaceComponents) {
            printWarning("Method does not have interface components, @Inline ignored: %s".formatted(method), method);
            return null;
        }

        var source = inline != null ? getSource(method, parameters) : null;
        var optimize = inline != null && !inline.getElementValues().isEmpty() && Boolean.TRUE.equals(inline.getElementValues().values().iterator().next().getValue());

        var methodData = new EntityProcessorMethod(method, method.getSimpleName().toString(), composition, parallel, parameters, source, optimize);
        return new SystemData(system, systemComposition, methodData);
    }

    private ParallelConfig resolveParallelStrategy(ExecutableElement method) {
        var strategy = ParallelStrategy.NONE;

        var annotation = resolveAnnotation(method, Utils.ENTITY_PROCESSOR);
        for (var entry : annotation.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals("parallel")) {
                strategy = ParallelStrategy.valueOf(entry.getValue().getValue().toString());
            }
        }

        return new ParallelConfig(strategy);
    }

    public ProcessorResult generate(ClassName className, SystemData system, HashMap<String, Integer> names) {
        if (system.methods().isEmpty()) {
            return ProcessorResult.EMPTY;
        }

        var result = new ProcessorResult();
        ProcessorMethod previousModifying = null;

        var methods = system.methods();
        for (int i = 0, s = methods.size(); i < s; i++) {
            var method = methods.get(i);

            switch (method) {
                case SystemProcessorMethod processor -> {
                    processSystemMethod(className, system, processor, names, result);
                    if (previousModifying == null) {
                        previousModifying = processor.modifying() ? processor : null;
                    }
                }
                case EntityProcessorMethod processor -> {
                    processEntityMethod(className, system, processor, names, result, previousModifying);
                    previousModifying = system.modifying() || processor.modifying() ? processor : null;
                }
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

    private void processEntityMethod(ClassName className, SystemData system, EntityProcessorMethod method, HashMap<String, Integer> names, ProcessorResult result, ProcessorMethod previousModifying) {
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
        result.fields.add(FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, type), fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
        result.fieldInit.addStatement("this.%s = new $1T<>($2T.class, 4)".formatted(fieldName), Utils.BAG, type);

        result.types.add(TypeGenerator.createType(className, system, method, type, generator));

        result.runImpl.add(switch (method.parallel().strategy()) {
            case NONE -> createRunImplementation(method, fieldName, type);
            case PER_ARCHETYPE -> createPerArchetypeRunImplementation(method, fieldName, type, result);
        });
    }

    private CodeBlock createRunImplementation(EntityProcessorMethod method, String fieldName, ClassName type) {
        var code = CodeBlock.builder();

        code.add("// %s".formatted(formatMethod(method))).add(System.lineSeparator());
        code.addStatement("$1T[] %1$sData = this.%1$s.getData()".formatted(fieldName), type);

        code.beginControlFlow("for (int i = 0, s = this.%s.size(); i<s; i++)".formatted(fieldName));
        code.addStatement("%sData[i].run()".formatted(fieldName));
        code.endControlFlow();

        return code.build();
    }

    private CodeBlock createPerArchetypeRunImplementation(EntityProcessorMethod method, String fieldName, ClassName type, ProcessorResult result) {
        var future = ParameterizedTypeName.get(ClassName.get(Future.class), Utils.WILDCARD);
        result.fields.add(FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, future), fieldName + "Futures", Modifier.PRIVATE, Modifier.FINAL).build());
        result.fieldInit.addStatement("this.%sFutures = new $1T<>($2T.class, 4)".formatted(fieldName), Utils.BAG, Future.class);

        if (!result.fields.contains(EXECUTOR)) {
            result.fields.add(EXECUTOR);
            result.fieldInit.addStatement("this._executor = $1T.newFixedThreadPool($2T.getRuntime().availableProcessors())", Executors.class, Runtime.class);
        }

        var code = CodeBlock.builder();
        code.add("// %s".formatted(formatMethod(method))).add(System.lineSeparator());

        code.addStatement("$1T[] %1$sData = this.%1$s.getData()".formatted(fieldName), type);
        code.addStatement("$1T<?>[] %1$sFutures = this.%1$sFutures.getData()".formatted(fieldName), Future.class);
        code.add(System.lineSeparator());

        code.addStatement("int s = this.%s.size()".formatted(fieldName));
        code.beginControlFlow("for (int i = 0; i < s; i++)");
        code.addStatement("%1$sFutures[i] = this._executor.submit(%1$sData[i])".formatted(fieldName));
        code.endControlFlow();
        code.beginControlFlow("for (int i = 0; i < s; i++)");
        code.beginControlFlow("try");
        code.addStatement("%1$sFutures[i].get()".formatted(fieldName));
        code.nextControlFlow("catch ($1T | $2T ex)", InterruptedException.class, ExecutionException.class);
        code.addStatement("throw new $1T(\"Parallel system invocation failed for method '%s' (PER_ARCHETYPE): \" + ex.getMessage(), ex)".formatted(method.methodName()), Utils.SYSTEM_INVOCATION_EXCEPTION);
        code.endControlFlow(); // try
        code.endControlFlow(); // for

        return code.build();
    }

    private String formatMethod(SystemMethod method) {
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

        public static TypeSpec createType(ClassName className, SystemData system, EntityProcessorMethod method, ClassName type, AbstractGenerator generator) {
            var composition = method.composition() != null ? method.composition() : system.composition();
            var parameters = method.parameters();

            var inline = method.source() != null;

            var fields = createFields(parameters, type);
            var componentFields = createComponentFields(parameters, composition, type, inline, generator);
            var entityParameter = parameters.stream().anyMatch(EntityParameter.class::isInstance);
            var run = inline ? runInlined(className, method, composition, entityParameter, generator) : run(className, method, composition, entityParameter, generator);

            var spec = TypeSpec.classBuilder(type)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addSuperinterface(Runnable.class)
                    .addField(Utils.buildComposition(composition))
                    .addField(Utils.WORLD, "_world", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.ENTITY_ARCHETYPE, "_archetype", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.INT_BAG, "_entities", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(className, "_system", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(fields)
                    .addFields(componentFields)
                    .addMethod(constructor(className, parameters, composition, entityParameter, inline, generator))
                    .addMethod(run);

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

        static List<FieldSpec> createComponentFields(List<ParameterData> parameters, CompositionData composition, ClassName processor, boolean inline, AbstractGenerator generator) {
            return parameters.stream()
                    .filter(ComponentParameter.class::isInstance)
                    .map(ComponentParameter.class::cast)
                    .flatMap(component -> component.fieldSpec(composition, inline, generator))
                    .distinct()
                    .toList();
        }

        static MethodSpec constructor(ClassName className, List<ParameterData> parameters, CompositionData composition, boolean entityParameter, boolean inline, AbstractGenerator generator) {
            var code = CodeBlock.builder();
            code.addStatement("this._world = invocation");
            code.addStatement("this._archetype = archetype");
            code.addStatement("this._entities = archetype.getEntities()");
            code.addStatement("this._system = system");

            code.add(System.lineSeparator());
            code.addStatement("$1T _data = archetype.getData()", Utils.ENTITY_ARCHETYPE_DATA_IMPL);

            var fieldProviders = parameters.stream()
                    .filter(FieldProvider.class::isInstance)
                    .map(FieldProvider.class::cast)
                    .filter(Utils.distinctBy(FieldProvider::fieldSpec))
                    .toList();

            if (!fieldProviders.isEmpty()) {
                code.add(System.lineSeparator());

                for (var fieldProvider : fieldProviders) {
                    fieldProvider.fieldInit(code, "invocation", "archetype");
                }
            }

            var components = parameters.stream()
                    .filter(ComponentParameter.class::isInstance)
                    .map(ComponentParameter.class::cast)
                    .toList();
            if (!components.isEmpty()) {
                code.add(System.lineSeparator());

                for (var component : components) {
                    component.fieldInit(code, "invocation", "archetype", "_data", composition, inline, generator);
                }
            }

            if (entityParameter) {
                code.add(System.lineSeparator());
                code.addStatement("this._entity = new $1T(archetype)", Utils.INTERNAL_ENTITY);
            }

            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(Utils.WORLD, "invocation")
                    .addParameter(Utils.ENTITY_ARCHETYPE, "archetype")
                    .addParameter(className, "system")
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec run(ClassName className, EntityProcessorMethod method, CompositionData composition, boolean entityParameter, AbstractGenerator generator) {
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

            code.addStatement("int[] _data = this._entities.getData()");
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

            var hasLoopInit = false;
            for (var parameter : parameters) {
                if (parameter instanceof LoopInitProvider loopInit) {
                    loopInit.loopInit(code, "_i", false);
                    hasLoopInit = true;
                }
            }

            if (hasLoopInit) {
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

            var hasCleanUp = false;
            for (var parameter : parameters) {
                if (parameter instanceof CleanUpProvider cleanUp) {
                    if (!hasCleanUp) {
                        code.add(System.lineSeparator());
                        hasCleanUp = true;
                    }

                    cleanUp.cleanUp(code, false);
                }
            }

            return MethodSpec.methodBuilder("run")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec runInlined(ClassName className, EntityProcessorMethod method, CompositionData composition, boolean entityParameter, AbstractGenerator generator) {
            var code = CodeBlock.builder();
            var parameters = method.parameters();
            var source = method.source();

            var javaDoc = CodeBlock.builder();
            javaDoc.add("Required imports:").add(System.lineSeparator());
            javaDoc.add(System.lineSeparator());

            var firstImport = true;
            for (var parameter : method.parameters()) {
                if (!firstImport) {
                    javaDoc.add(", ");
                }
                firstImport = false;

                if (!parameter.type().isPrimitive()) {
                    javaDoc.add("$1T", parameter.type());
                }
            }
            for (var name : source.imports()) {
                if (!firstImport) {
                    javaDoc.add(", ");
                }
                firstImport = false;

                javaDoc.add("$1T", ClassName.bestGuess(name));
            }

            var sourceCode = source.source();
            code.add("/* Original:").add(System.lineSeparator());
            code.add(sourceCode);
            code.add(System.lineSeparator()).add("*/").add(System.lineSeparator());
            code.add(System.lineSeparator());

            var inlinedSource = inlineSource(sourceCode, method.parameters(), method.optimize(), generator);
            code.add("/* Modified:").add(System.lineSeparator());
            code.add(inlinedSource);
            code.add(System.lineSeparator()).add("*/").add(System.lineSeparator());
            code.add(System.lineSeparator());

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

                if (parameter instanceof ComponentParameter c && c.data() instanceof InterfaceComponent component) {
                    for (var field : component.fields()) {
                        var fieldSpec = EntityArchetypeDataGenerator.createInterfaceComponentField(component, field, generator);
                        code.addStatement("$1T %1$s = this.%1$s.getData()".formatted(fieldSpec.field().name()), fieldSpec.dataType());
                    }

                    hasLocalVars = true;
                }

                if (parameter instanceof FieldProvider f && !f.fieldSpec().name().equals(parameter.name())) {
                    code.addStatement("$1T %s = this.%s".formatted(parameter.name(), f.fieldSpec().name()), parameter.type());

                    hasLocalVars = true;
                }
            }

            if (hasLocalVars) {
                code.add(System.lineSeparator());
            }

            code.addStatement("int[] _data = this._entities.getData()");
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

            var hasLoopInit = false;
            for (var parameter : parameters) {
                if (parameter instanceof LoopInitProvider loopInit) {
                    loopInit.loopInit(code, "_i", true);
                    hasLoopInit = true;
                }
            }

            if (hasLoopInit) {
                code.add(System.lineSeparator());
            }

            code.add(inlinedSource).add(System.lineSeparator());

            code.endControlFlow();

            if (entityParameter) {
                // Reset entity fields to -1 to cause errors on improper use outside of the main loop
                code.add(System.lineSeparator());
                code.add("// Reset state to avoid improper use as Entity outside of the loop").add(System.lineSeparator());
                code.addStatement("_entity.index = -1");
                code.addStatement("_entity.entityId = -1");
            }

            var hasCleanUp = false;
            for (var parameter : parameters) {
                if (parameter instanceof CleanUpProvider cleanUp) {
                    if (!hasCleanUp) {
                        code.add(System.lineSeparator());
                        hasCleanUp = true;
                    }

                    cleanUp.cleanUp(code, true);
                }
            }

            return MethodSpec.methodBuilder("run")
                    .addJavadoc(javaDoc.build())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addCode(code.build())
                    .build();
        }

        private static String inlineSource(String source, List<ParameterData> parameters, boolean optimize, AbstractGenerator generator) {
            for (var parameter : parameters) {
                if (parameter instanceof ComponentParameter p && p.data() instanceof InterfaceComponent component) {
                    for (var field : component.fields()) {
                        var fieldSpec = EntityArchetypeDataGenerator.createInterfaceComponentField(component, field, generator);

                        var getter = Pattern.compile("%s.%s\\(\\)".formatted(p.name(), field.name()));
                        source = getter.matcher(source).replaceAll("%s[_i]".formatted(fieldSpec.field().name()));

                        var setter = Pattern.compile("%s.%s\\((.+)\\)".formatted(p.name(), field.name()));
                        source = setter.matcher(source).replaceAll("%s[_i] = $1".formatted(fieldSpec.field().name()));

                        if (optimize) {
                            var lines = source.split("\\R");
                            for (int i = 0, s = lines.length; i < s; i++) {
                                lines[i] = InlineOptimizations.optimizeAll(lines[i]);
                            }

                            source = Arrays.stream(lines).collect(Collectors.joining(System.lineSeparator()));
                        }
                    }
                }
            }

            return source;
        }

        private enum InlineOptimizations {

            COMPOUND_ADD("+"),
            COMPOUND_SUBTRACT("-"),
            COMPOUND_MULTIPLY("*"),
            COMPOUND_DIVIDE("/"),
            COMPOUND_MODULO("%"),
            COMPOUND_AND("&"),
            COMPOUND_OR("|"),
            COMPOUND_XOR("^"),
            COMPOUND_SHIFT_LEFT("<<"),
            COMPOUND_ARITHMETIC_SHIFT_RIGHT(">>"),
            COMPOUND_LOGICAL_SHIFT_RIGHT(">>>");

            private static final InlineOptimizations[] VALUES = values();

            private static final String EXPR = "[a-zA-Z_][a-zA-Z0-9_\\[\\]\\.\\(\\)\\s\\+\\-\\*/]*";

            private static String regex(String operator) {
                return String.format("""
                                (?<indent>)\\s*\
                                (?<left>%s)\\s*=\\s*\
                                (?<right>%s)\\s*%s\\s*\
                                (?<expr>[^;]+)""",
                        EXPR,
                        EXPR,
                        Pattern.quote(operator));
            }

            private final Pattern pattern;
            private final String operator;

            private InlineOptimizations(String operator) {
                this.pattern = Pattern.compile(regex(operator));
                this.operator = operator;
            }

            static String optimizeAll(String line) {
                for (var optimization : VALUES) {
                    line = optimization.optimize(line);
                }

                return line;
            }

            private String optimize(String line) {
                var matcher = this.pattern.matcher(line);
                var builder = new StringBuilder();

                while (matcher.find()) {
                    var left = matcher.group("left").trim();
                    var right = matcher.group("right").trim();

                    if (left.equals(right)) {
                        var indent = matcher.group("indent");
                        var expr = matcher.group("expr").trim();

                        var replacement = indent + left + " " + operator + "= " + expr;
                        matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
                    } else {
                        matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(0)));
                    }
                }

                matcher.appendTail(builder);
                return builder.toString();
            }

        }

    }

}
