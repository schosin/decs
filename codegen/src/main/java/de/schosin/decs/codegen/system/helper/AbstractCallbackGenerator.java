package de.schosin.decs.codegen.system.helper;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.TypeData.SystemData;
import de.schosin.decs.codegen.system.methods.CallbackMethod;
import de.schosin.decs.codegen.utils.Annotations;
import de.schosin.decs.codegen.utils.ParameterData;
import de.schosin.decs.codegen.utils.ParameterData.EntityParameter;
import de.schosin.decs.codegen.utils.ParameterData.LocalVariableProvider;
import de.schosin.decs.codegen.utils.Utils;

abstract class AbstractCallbackGenerator<M extends CallbackMethod> extends AbstractSystemGenerator {

    private final TypeElement annotation;
    private final String name;

    private final Class<? extends M> methodClass;
    private final String typeSuffix;

    public AbstractCallbackGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator, Function<Annotations, TypeElement> annotation,
            Class<? extends M> methodClass, String typeSuffix) {
        super(processingEnv, roundEnv, generator);

        this.annotation = annotation.apply(annotations);
        this.name = this.annotation.getSimpleName().toString();

        this.methodClass = methodClass;
        this.typeSuffix = typeSuffix;
    }

    public Stream<SystemData> detect() {
        return roundEnv.getElementsAnnotatedWith(annotation).stream()
                .map(ExecutableElement.class::cast)
                .map(this::handleMethod);
    }

    private SystemData handleMethod(ExecutableElement method) {
        var system = getSystemType(method, name);
        if (system == null) {
            return null;
        }

        if (!method.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @%s must not have type parameters.".formatted(name), method);
            return null;
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            printError("Methods annotated with @%s must return void.".formatted(name), method);
            return null;
        }

        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            printError("Methods annotated with @%s must not be private.".formatted(name), method);
            return null;
        }

        if (!method.getModifiers().contains(Modifier.FINAL)) {
            printError("Methods annotated with @%s must be final.".formatted(name), method);
            return null;
        }

        var parameters = resolveCallbackParameters(method, name);
        if (parameters == null) {
            return null;
        }

        var systemComposition = CompositionData.detect(system, generator);
        var composition = CompositionData.detect(method, generator);
        if (!validateComposition(method, parameters, composition != null ? composition : systemComposition, this.name)) {
            return null;
        }

        var methodData = createMethod(method, composition, parameters);
        return new SystemData(system, systemComposition, methodData);
    }

    protected List<ParameterData> resolveCallbackParameters(ExecutableElement method, String name) {
        return resolveProcessorParameters(method, name);
    }

    protected abstract M createMethod(ExecutableElement method, CompositionData composition, List<ParameterData> parameters);

    public GeneratorResult generate(ClassName className, SystemData system, HashMap<String, Integer> names) {
        var methods = system.callbacks().stream()
                .filter(methodClass::isInstance)
                .map(methodClass::cast)
                .toList();

        if (methods.isEmpty()) {
            return GeneratorResult.EMPTY;
        }

        var result = new GeneratorResult();

        result.fieldInit().add(System.lineSeparator());
        for (var method : methods) {
            var originalName = method.methodName() + this.typeSuffix;

            // Resolve final name in case of FFFF
            var fieldName = originalName;
            int count = names.merge(fieldName, 1, Integer::sum);

            while (count > 1) {
                fieldName = originalName + count;
                count = names.merge(fieldName, 1, Integer::sum);
            }

            var typeName = Utils.capitalize(fieldName);
            var type = ClassName.get("", typeName);

            // Generate code
            result.types().add(TypeGenerator.createType(className, system, method, type));
            result.fieldInit().addStatement("invocation.%s($1T.COMPOSITION, archetype -> new $1T(invocation, archetype, this))".formatted(name.toLowerCase()), type);
        }

        return result;
    }

    private static class TypeGenerator {

        public static TypeSpec createType(ClassName className, SystemData system, CallbackMethod method, ClassName type) {
            var composition = method.composition() != null ? method.composition() : system.composition();
            var parameters = method.parameters();

            var fields = ProcessorGenerator.TypeGenerator.createFields(parameters, type);
            var componentFields = ProcessorGenerator.TypeGenerator.createComponentFields(parameters, composition, type, false, null);
            var entityParameter = parameters.stream().anyMatch(EntityParameter.class::isInstance);

            var spec = TypeSpec.classBuilder(type)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addSuperinterface(Utils.ENTITY_ARCHETYPE_LISTENER)
                    .addField(Utils.buildComposition(composition))
                    .addField(Utils.WORLD, "_world", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.ENTITY_ARCHETYPE, "_archetype", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(Utils.INT_BAG, "_entities", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(className, "_system", Modifier.PRIVATE, Modifier.FINAL)
                    .addFields(fields)
                    .addFields(componentFields)
                    .addMethod(ProcessorGenerator.TypeGenerator.constructor(className, parameters, composition, entityParameter, false, null))
                    .addMethod(processRange(className, method, composition, entityParameter))
                    .addMethod(processBag(className, method, composition, entityParameter));

            if (entityParameter) {
                spec.addField(FieldSpec.builder(Utils.INTERNAL_ENTITY, "_entity", Modifier.PRIVATE, Modifier.FINAL).build());
            }

            return spec.build();
        }

        private static MethodSpec processRange(ClassName className, CallbackMethod method, CompositionData composition, boolean entityParameter) {
            var code = CodeBlock.builder();
            var parameters = method.parameters();

            // Local variables
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

            // Method call
            code.addStatement("int[] _data = this._entities.getData()");
            if (entityParameter) {
                code.addStatement("$1T _entity = this._entity", Utils.INTERNAL_ENTITY);
            }

            code.add(System.lineSeparator());
            code.beginControlFlow("for (int _i = _start; _i < _end; _i++)");

            if (entityParameter) {
                code.addStatement("_entity.index = _i");
                code.addStatement("_entity.entityId = _data[_i]");
                code.add(System.lineSeparator());
            }

            code.add("_system.%s(".formatted(method.methodName()));
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

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(TypeName.INT, "_start")
                    .addParameter(TypeName.INT, "_end")
                    .addCode(code.build())
                    .build();
        }

        private static MethodSpec processBag(ClassName className, CallbackMethod method, CompositionData composition, boolean entityParameter) {
            var code = CodeBlock.builder();
            var parameters = method.parameters();

            // Local variables
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

            // Method call
            code.addStatement("int[] _data = this._entities.getData()");
            code.addStatement("int[] _indicesData = _indices.getData()");
            if (entityParameter) {
                code.addStatement("$1T _entity = this._entity", Utils.INTERNAL_ENTITY);
            }

            code.add(System.lineSeparator());
            code.beginControlFlow("for (int _i = 0, _s = _indices.size(); _i < _s; _i++)");

            code.addStatement("int _index = _indicesData[_i]");

            if (entityParameter) {
                code.add(System.lineSeparator());
                code.addStatement("_entity.index = _i");
                code.addStatement("_entity.entityId = _data[_i]");
            }

            code.add(System.lineSeparator());
            code.add("this._system.%s(".formatted(method.methodName()));
            for (int i = 0, s = parameters.size(); i < s; i++) {
                var parameter = parameters.get(i);

                if (i > 0) {
                    code.add(", ");
                }

                parameter.entityAccessor(code, "_data", "_index", composition);
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

            return MethodSpec.methodBuilder("process")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addParameter(Utils.INT_BAG, "_indices")
                    .addCode(code.build())
                    .build();
        }

    }

}
