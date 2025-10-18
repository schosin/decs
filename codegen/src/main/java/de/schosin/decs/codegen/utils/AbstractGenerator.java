package de.schosin.decs.codegen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.utils.ParameterData.*;
import de.schosin.decs.codegen.utils.source.SourceProvider;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;

public abstract class AbstractGenerator {

    private final Messager messager;

    public final ProcessingEnvironment processingEnv;
    public final RoundEnvironment roundEnv;
    public final Annotations annotations;

    public final Types types;
    public final Elements elements;
    public final SourceProvider sourceProvider;

    public final ComponentsResult components;

    private boolean error;

    public AbstractGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components) {
        this.messager = processingEnv.getMessager();

        this.processingEnv = processingEnv;
        this.roundEnv = roundEnv;
        this.annotations = Utils.getAnnotations(processingEnv);

        this.types = processingEnv.getTypeUtils();
        this.elements = processingEnv.getElementUtils();
        this.sourceProvider = SourceProvider.getInstance(processingEnv);

        this.components = components;
    }

    public boolean isError() {
        return this.error;
    }

    public boolean printError(String msg) {
        this.error = true;
        messager.printError(msg);

        return true;
    }

    public boolean printError(String msg, Element element) {
        this.error = true;

        if (element != null) {
            messager.printError(msg, element);
        } else {
            messager.printError(msg);
        }

        return true;
    }

    public void printWarning(String msg) {
        messager.printWarning(msg);
    }

    public void printWarning(String msg, Element element) {
        if (element != null) {
            messager.printWarning(msg, element);
        } else {
            messager.printWarning(msg);
        }
    }

    public void printNote(String msg) {
        messager.printNote(msg);
    }

    public void printNote(String msg, Element element) {
        if (element != null) {
            messager.printNote(msg, element);
        } else {
            messager.printNote(msg);
        }
    }

    protected boolean isPublicAccessible(TypeElement element) {
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            printError("Type must be public.", element);
            return false;
        }

        var parent = element.getEnclosingElement();
        while (parent instanceof TypeElement parentElement && parent.getKind() != ElementKind.PACKAGE) {
            if (!element.getModifiers().contains(Modifier.STATIC)) {
                printError("Nested type must be static.", element);
                return false;
            }

            if (!parent.getModifiers().contains(Modifier.PUBLIC)) {
                printError("Parent type must be public.", element);
                return false;
            }

            element = parentElement;
            parent = parentElement.getEnclosingElement();
        }

        return true;
    }

    protected String getPackageName(Element element) {
        var work = element;
        while (work != null && work.getKind() != ElementKind.PACKAGE) {
            work = work.getEnclosingElement();
        }

        if (work instanceof PackageElement pkg) {
            return pkg.getQualifiedName().toString();
        }

        printError("Failed to resolve package name for element of type %s: %s".formatted(element.getKind(), element), element);
        return null;
    }

    protected List<ParameterData> resolveParameters(ExecutableElement method) {
        return method.getParameters().stream()
                .map(parameter -> resolveParameter(method, parameter))
                .toList();
    }

    protected ParameterData resolveParameter(ExecutableElement method, VariableElement parameter) {
        var name = parameter.getSimpleName().toString();
        if (!validateParameter(method, parameter)) {
            return new InvalidParameter(parameter, name);
        }

        var type = parameter.asType();

        // Resolve @Value parameter
        var value = resolveAnnotation(parameter, Utils.VALUE);
        if (value != null) {
            return new ValueParameter(parameter, name, TypeName.get(type), (String) value.getElementValues().values().iterator().next().getValue());
        }

        if (type instanceof PrimitiveType primitive) {
            return resolvePrimitiveParameter(method, parameter, primitive);
        }

        if (type instanceof DeclaredType declared) {
            return resolveDeclaredParameter(method, parameter, declared);
        }

        printError("Invalid parameter '%s' of method '%s': Must be a primitive (int, long, float, double), a declared reference type (interface, class, enum, record), or annotated with @Value"
                .formatted(name, format(method)), parameter);

        return new InvalidParameter(parameter, name);
    }

    private boolean validateParameter(ExecutableElement method, VariableElement parameter) {
        var name = parameter.getSimpleName().toString();

        if (name.startsWith("_")) {
            printError("Invalid parameter '%s' of method '%s': Parameters must not start with an underscore (_)".formatted(name, format(method)), parameter);
            return false;
        }

        return true;
    }

    private ParameterData resolvePrimitiveParameter(ExecutableElement method, VariableElement parameter, PrimitiveType primitive) {
        var name = parameter.getSimpleName().toString();

        var value = resolveAnnotation(parameter, Utils.VALUE);
        if (value == null) {
            if (primitive.getKind() == TypeKind.INT) {
                return new EntityIdParameter(parameter, name);
            }

            printError("Invalid primitive parameter '%s': Primitive values are only supported with @Value".formatted(name), parameter);
            return new InvalidParameter(parameter, name);
        }

        return switch (primitive.getKind()) {
            case INT, LONG, FLOAT, DOUBLE ->
                    new ValueParameter(parameter, name, TypeName.get(primitive), (String) value.getElementValues().values().iterator().next().getValue());
            default -> {
                printError("Unsupported primitive type for parameter '%s' of method '%s'. Must be int, long, float or double.".formatted(name, format(method)), parameter);
                yield new InvalidParameter(parameter, name);
            }
        };
    }

    private ParameterData resolveDeclaredParameter(ExecutableElement method, VariableElement parameter, DeclaredType declared) {
        var name = parameter.getSimpleName().toString();
        var type = ClassName.get((TypeElement) declared.asElement());

        // Disallow usage of InternalWorld
        if (Utils.INTERNAL_WORLD.canonicalName().equals(declared.toString())) {
            printError("Cannot use InternalWorld.".formatted(name), parameter);
            return new InvalidParameter(parameter, name);
        }

        // Disallow usage of InternalEntity
        if (Utils.INTERNAL_ENTITY.canonicalName().equals(declared.toString())) {
            printError("Cannot use InternalEntity.".formatted(name), parameter);
            return new InvalidParameter(parameter, name);
        }

        // Handle World parameter
        if (Utils.WORLD.canonicalName().equals(declared.toString())) {
            return new WorldParameter(parameter, name);
        }

        // Handle Entity parameter
        if (Utils.ENTITY.canonicalName().equals(declared.toString())) {
            return new EntityParameter(parameter, name);
        }

        // Resolve @Component parameter
        var component = components.getReadComponent(declared);
        if (component != null) {
            return new ComponentParameter(parameter, component);
        }

        // Check for system processor methods
        var hasSystemMethods = hasSystemMethods(parameter.asType());
        if (hasSystemMethods) {
            printError("Detected system type parameter. Cannot use systems as parameters.".formatted(name), parameter);
            return new InvalidParameter(parameter, name);
        }

        // Resolve utility parameter
        var utility = resolveAnnotation(parameter, Utils.UTILITY);
        if (utility != null) {
            return new UtilityParameter(parameter, name, type);
        }

        // Check for utility methods
        var hasUtilityMethods = hasUtilityMethods(parameter.asType());
        if (hasUtilityMethods) {
            printWarning("Detected utility type parameter '%s'. Annotate parameter with @Utility.".formatted(name), parameter);
            return new UtilityParameter(parameter, name, type);
        }

        // Resolve singleton parameter
        var singleton = resolveAnnotation(parameter, Utils.SINGLETON);
        if (singleton != null) {
            return new SingletonParameter(parameter, name, type);
        }

        // Handle invalid parameter
        printError("Invalid parameter type. See JavaDoc for annotation to see supported parameter types.", parameter);
        return new InvalidParameter(parameter, name);
    }

    protected boolean hasSystemMethods(TypeMirror type) {
        return hasAnnotatedMethod(type, annotations.getSystemMethodAnnotations());
    }

    protected boolean hasUtilityMethods(TypeMirror type) {
        return hasAnnotatedMethod(type, annotations.getUtilityMethodAnnotations());
    }

    private boolean hasAnnotatedMethod(TypeMirror type, Set<DeclaredType> annotations) {
        return processingEnv.getTypeUtils().asElement(type) instanceof TypeElement element && element.getEnclosedElements().stream()
                .anyMatch(elem -> elem.getAnnotationMirrors().stream()
                        .anyMatch(annotation -> annotations.contains(annotation.getAnnotationType())));
    }

    protected AnnotationMirror resolveAnnotation(Element element, ClassName type) {
        var value = element.getAnnotationMirrors().stream()
                .filter(ann -> ann.getAnnotationType().toString().equals(type.canonicalName()))
                .findFirst()
                .orElse(null);

        if (value != null) {
            return value;
        }

        for (var annotation : element.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().startsWith("java.lang.")) {
                continue;
            }

            value = resolveAnnotation(annotation.getAnnotationType(), type);
            if (value != null) {
                return value;
            }

            var resolvedAnnotation = processingEnv.getElementUtils().getTypeElement(annotation.getAnnotationType().toString());
            value = resolveAnnotation(resolvedAnnotation, type);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private AnnotationMirror resolveAnnotation(DeclaredType annotationType, ClassName type) {
        var value = annotationType.getAnnotationMirrors().stream()
                .filter(ann -> ann.getAnnotationType().toString().equals(type.canonicalName()))
                .findFirst()
                .orElse(null);

        if (value != null) {
            return value;
        }

        for (var annotation : annotationType.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().startsWith("java.lang.")) {
                continue;
            }

            value = resolveAnnotation(annotation.getAnnotationType(), type);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    protected String format(ExecutableElement method) {
        var enclosing = method.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof TypeElement)) {
            enclosing = enclosing.getEnclosingElement();
        }

        if (enclosing == null) {
            return method.toString();
        }

        return "%s#%s".formatted(enclosing, method);
    }

    protected static boolean isOfKind(VariableElement variable, ElementKind kind) {
        return isOfKind(variable.asType(), kind);
    }

    protected static boolean isOfKind(TypeMirror type, ElementKind kind) {
        return type instanceof DeclaredType declared
                && declared.asElement() instanceof TypeElement element
                && element.getKind() == kind;
    }

}
