package de.schosin.decs.codegen.system.helper;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.ParameterData;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;
import de.schosin.decs.codegen.utils.ParameterData.EntityParameterData;
import de.schosin.decs.codegen.utils.Utils;

abstract class AbstractSystemGenerator extends AbstractGenerator {

    protected final SystemGenerator generator;

    private final Set<TypeElement> seenTypes;
    private final Set<TypeElement> seenSystems;

    private final TypeMirror entityArchetype = elements.getTypeElement(Utils.ENTITY_ARCHETYPE.canonicalName()).asType();

    public AbstractSystemGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator.components);

        this.generator = generator;

        this.seenTypes = generator.seenTypes;
        this.seenSystems = generator.seenSystems;
    }

    protected TypeElement getSystemType(ExecutableElement method, String annotation) {
        var system = getParentType(method, annotation);
        if (system == null) {
            return null;
        }

        // Skip validation if already seen to avoid duplicate errors
        if (!seenSystems.add(system)) {
            return system;
        }

        if (system.getKind() != ElementKind.CLASS) {
            printError("Types using @%s must be classes.".formatted(annotation), system);
            return null;
        }

        var offerArchetypeMethod = detectOfferArchetypeMethod(system);
        if (offerArchetypeMethod != null) {
            printError("Types using @%s must not have a method called \"offerArchetype\" with a EntityArchetype argument.".formatted(annotation), offerArchetypeMethod);
            return null;
        }

        var runSystemMethod = detectRunSystemMethod(system);
        if (runSystemMethod != null) {
            printError("Types using @%s must not have a method called \"runSystem\" with no arguments.".formatted(annotation), runSystemMethod);
            return null;
        }

        if (implementsCallable(system)) {
            printError("Types using @%s must not implement Callable.".formatted(annotation), system);
            return null;
        }

        var callMethod = detectCallMethod(system);
        if (callMethod != null) {
            printError("Types using @%s must not have a method called \"call\" with no arguments (Callable signature).".formatted(annotation), callMethod);
            return null;
        }

        var constructors = system.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .toList();

        // TODO consider to allow constructor DI with SystemParameterData or a subset thereof
        if (!constructors.isEmpty()) {
            var error = false;
            for (var constructor : constructors) {
                if (constructor.getModifiers().contains(Modifier.PRIVATE) || !constructor.getParameters().isEmpty()) {
                    error = true;
                    printError("Types using @%s must not declare constructors.".formatted(annotation), constructor);
                }
            }

            if (error) {
                return null;
            }
        }

        return system;
    }

    private ExecutableElement detectOfferArchetypeMethod(TypeElement system) {
        return system.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getSimpleName().contentEquals("offerArchetype")
                        && method.getParameters().size() == 1
                        && method.getParameters().get(0).asType().equals(entityArchetype))
                .findFirst()
                .orElse(null);
    }

    private ExecutableElement detectRunSystemMethod(TypeElement system) {
        return system.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getSimpleName().contentEquals("runSystem") && method.getParameters().isEmpty())
                .findFirst()
                .orElse(null);
    }

    private boolean implementsCallable(TypeElement system) {
        return system.getInterfaces().stream()
                .map(DeclaredType.class::cast)
                .map(DeclaredType::asElement)
                .map(TypeElement.class::cast)
                .anyMatch(type -> Callable.class.getName().equals(type.getQualifiedName().toString()));
    }

    private ExecutableElement detectCallMethod(TypeElement system) {
        return system.getEnclosedElements().stream()
                .filter(ExecutableElement.class::isInstance)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getSimpleName().contentEquals("call") && method.getParameters().isEmpty())
                .findFirst()
                .orElse(null);
    }

    protected TypeElement getParentType(ExecutableElement method, String annotation) {
        var system = (TypeElement) method.getEnclosingElement();

        // Skip validation if already seen to avoid duplicate errors
        if (!seenTypes.add(system)) {
            return system;
        }

        if (!system.getTypeParameters().isEmpty()) {
            printError("Types using @%s must not have type parameters.".formatted(annotation), system);
            return null;
        }

        if (!system.getModifiers().contains(Modifier.PUBLIC)) {
            printError("Types using @%s must be public.".formatted(annotation), system);
            return null;
        }

        if (system.getKind() != ElementKind.CLASS) {
            printError("Types using @%s must be an abstract class.".formatted(annotation), system);
            return null;
        }

        if (system.getModifiers().contains(Modifier.FINAL)) {
            printError("Types using @%s must not be final.".formatted(annotation), system);
            return null;
        }

        var parent = system.getEnclosingElement();
        while (parent instanceof TypeElement parentElement) {
            if (!system.getModifiers().contains(Modifier.STATIC)) {
                printError("Types using @%s must be accessible. Nested types must be static.".formatted(annotation), system);
                return null;
            }

            if (!parentElement.getModifiers().contains(Modifier.PUBLIC)) {
                printError("Types using @%s must be accessible. Type is nested in a non-public parent type.".formatted(annotation), system);
                return null;
            }

            parent = parent.getEnclosingElement();
        }

        return system;
    }

    protected List<ParameterData> resolveProcessorParameters(ExecutableElement method, String annotation) {
        var parameters = resolveParameters(method);

        var hasEntityParameters = parameters.stream().anyMatch(EntityParameterData.class::isInstance);
        if (!hasEntityParameters) {
            printNote("@%s method does not use any entity data.".formatted(annotation), method);
        }

        return parameters;
    }

    protected boolean validateComposition(ExecutableElement method, List<ParameterData> parameters, CompositionData composition, String annotation) {
        if (composition == null) {
            printError("@%s method or its owning class must be annotated with @All/@One/@None.".formatted(annotation), method);
            return false;
        }

        var none = composition.none();
        if (none == null || none.isEmpty()) {
            return true;
        }

        var valid = true;
        for (var excluded : none) {
            for (var parameter : parameters) {
                if (parameter instanceof ComponentParameter component && component.type().equals(excluded)) {
                    printError("Parameter is excluded by @None and will only ever be null.", parameter.parameter());
                    valid = false;
                }
            }
        }

        return valid;
    }

    @Override
    public boolean isError() {
        return this.generator.isError();
    }

    @Override
    public boolean printError(String msg, Element element) {
        return this.generator.printError(msg, element);
    }

    @Override
    public void printWarning(String msg) {
        this.generator.printWarning(msg);
    }

    @Override
    public void printWarning(String msg, Element element) {
        this.generator.printWarning(msg, element);
    }

    @Override
    public void printNote(String msg) {
        this.generator.printNote(msg);
    }

    @Override
    public void printNote(String msg, Element element) {
        this.generator.printNote(msg, element);
    }

}
