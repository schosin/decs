package de.schosin.decs.codegen.system.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.palantir.javapoet.ClassName;

import de.schosin.decs.codegen.components.ComponentData.EnumComponentData;
import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;

public abstract class AbstractUtilityGenerator extends AbstractSystemGenerator {

    private final String annotation;
    private final Set<TypeElement> seenUtilities;

    public AbstractUtilityGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator, String annotation) {
        super(processingEnv, roundEnv, generator);

        this.annotation = annotation;
        this.seenUtilities = generator.seenUtilities;
    }

    protected final ExecutableElement resolveInitializerMethod(TypeElement system, ExecutableElement method) {
        var initializer = system.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .dropWhile(element -> !method.equals(element))
                .skip(1L)
                .findFirst()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(executable -> executable.getAnnotationMirrors().stream().anyMatch(annotation -> annotation.getAnnotationType().equals(annotations.entityInitializerType())))
                .orElse(null);

        if (initializer == null) {
            printError("@%s method must be immediately followed by a matching @EntityInitializer method.".formatted(annotation), method);
            return null;
        }

        if (initializer.getModifiers().contains(Modifier.DEFAULT)) {
            // "must be abstract" for type enough
            return null;
        }

        var error = false;

        if (!initializer.getTypeParameters().isEmpty()) {
            printError("Methods annotated with @EntityInitializer must not have type parameters.", method);
            return null;
        }

        if (initializer.getReturnType().getKind() != TypeKind.VOID) {
            printError("Methods annotated with @EntityInitializer must return void.", method);
            error = true;
        }

        if (initializer.getModifiers().contains(Modifier.PRIVATE)) {
            printError("Methods annotated with @EntityInitializer must not be private.", initializer);
            error = true;
        }

        if (!initializer.getModifiers().contains(Modifier.FINAL)) {
            printError("Methods annotated with @EntityInitializer must be final.", initializer);
            error = true;
        }

        var composition = CompositionData.detect(initializer, generator);
        if (composition != null) {
            printError("Cannot use composition annotations on @EntityInitializer for a @Transmute method.", initializer);
            error = true;
        }

        if (error) {
            return null;
        }

        return initializer;
    }

    protected List<ComponentParameter> resolveComponents(ExecutableElement method, ExecutableElement initializer, List<Parameter> parameters, List<? extends VariableElement> enumParameters) {
        var error = false;
        var seen = new HashSet<ClassName>();

        var result = new ArrayList<ComponentParameter>();
        for (int i = 1 + parameters.size() + enumParameters.size(), s = initializer.getParameters().size(); i < s; i++) {
            var parameter = initializer.getParameters().get(i);
            var type = parameter.asType();

            var component = components.getWrittenComponent(type);
            if (component == null) {
                // Handle primitives
                if (type.getKind().isPrimitive()) {
                    printError("Primitive \"%s\" cannot be used as a component. Remove the parameter.".formatted(type), parameter);

                    error = true;
                    continue;
                }

                // Handle non-source types
                if (!isSourceType(type)) {
                    printError("Non-source type \"%s\" cannot be used as a component. Remove the parameter.".formatted(type), parameter);

                    error = true;
                    continue;
                }

                // Handle source types
                printError("Type \"%s\" is not a component. Remove the parameter, or annotate it with @Component.".formatted(type), parameter);

                error = true;
                continue;
            }

            if (component instanceof EnumComponentData) {
                printError("Enum components must be set by the @%s method.".formatted(this.annotation), parameter);

                error = true;
                continue;
            }

            if (!seen.add(component.className())) {
                printError("The same component type cannot be used multiple times: %s".formatted(component.type()), parameter);

                error = true;
                continue;
            }

            result.add(new ComponentParameter(parameter, component));
        }

        if (error) {
            return null;
        }

        return result;
    }

    protected TypeElement getUtilityType(ExecutableElement method, String annotation) {
        var utility = getParentType(method, annotation);
        if (utility == null) {
            return null;
        }

        // Skip validation if already seen to avoid duplicate errors
        if (!seenUtilities.add(utility)) {
            return utility;
        }

        if (!utility.getModifiers().contains(Modifier.ABSTRACT)) {
            printError("Types using @%s must be an abstract class.".formatted(annotation), utility);
            return null;
        }

        return utility;
    }

    protected boolean isSourceType(TypeMirror type) {
        if (!(types.asElement(type) instanceof TypeElement element)) {
            return false;
        }

        var parent = element.getEnclosingElement();
        while (parent != null && parent.getKind() != ElementKind.PACKAGE) {
            parent = parent.getEnclosingElement();
        }

        var elementName = processingEnv.getElementUtils().getBinaryName(element);
        for (var root : roundEnv.getRootElements()) {
            var rootName = processingEnv.getElementUtils().getBinaryName((TypeElement) root);

            if (rootName.contentEquals(elementName)) {
                return true;
            }
        }

        return false;
    }

}
