package de.schosin.decs.codegen.system;

import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;

import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.Utils;

public record CompositionData(List<TypeName> all, List<List<TypeName>> ones, List<TypeName> none) {

    public CompositionData {
        if (all != null && all.isEmpty() && ((ones != null && !ones.isEmpty()) || (none != null && !none.isEmpty()))) {
            throw new IllegalStateException("@All can only omit components when @One and @None are not used. Declare atleast one component or remove @One and @None.");
        }

        if (ones != null && ones.isEmpty()) {
            throw new IllegalStateException("@Ones cannot be empty.");
        }

        if (none != null && none.isEmpty()) {
            throw new IllegalStateException("@None cannot be empty.");
        }
    }

    public static CompositionData detect(TypeElement type, AbstractGenerator generator) {
        return detectCompositionData(type, generator);
    }

    public static CompositionData detect(ExecutableElement method, AbstractGenerator generator) {
        return detectCompositionData(method, generator);
    }

    private static CompositionData detectCompositionData(Element element, AbstractGenerator generator) {
        if (element == null) {
            return null;
        }

        var all = detectAll(element, generator);
        var ones = detectOnes(element, generator);
        var none = detectNone(element, generator);

        if (all == null && ones == null && none == null) {
            return null;
        }

        // @All without components only allowed by itself
        if (all != null && all.isEmpty() && ((ones != null && !ones.isEmpty()) || (none != null && !none.isEmpty()))) {
            generator.printError("@All can only omit components when @One and @None are not used. Declare atleast one component or remove @One and @None.", element);
            return new CompositionData(all, ones, none);
        }

        return new CompositionData(all, ones, none);
    }

    public boolean isAllEntities() {
        return all != null && all.isEmpty() && ones == null && none == null;
    }

    @SuppressWarnings("unchecked")
    private static List<TypeName> detectAll(Element element, AbstractGenerator generator) {
        var annotation = element.getAnnotationMirrors().stream()
                .filter(ann -> ann.getAnnotationType().toString().equals(Utils.ALL.canonicalName()))
                .findFirst()
                .orElse(null);

        if (annotation == null) {
            return null;
        }

        if (annotation.getElementValues().isEmpty()) {
            return List.of();
        }

        var components = (List<AnnotationValue>) annotation.getElementValues().values().iterator().next().getValue();
        if (components.isEmpty()) {
            return List.of();
        }

        return components.stream()
                .map(AnnotationValue::getValue)
                .map(DeclaredType.class::cast)
                .flatMap(component -> processComponent(element, component, generator))
                .distinct()
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<List<TypeName>> detectOnes(Element element, AbstractGenerator generator) {
        var annotations = Stream.concat(
                // @One
                element.getAnnotationMirrors().stream()
                        .filter(ann -> ann.getAnnotationType().toString().equals(Utils.ONE.canonicalName())),
                // @Ones
                element.getAnnotationMirrors().stream()
                        .filter(ann -> ann.getAnnotationType().toString().equals(Utils.ONES.canonicalName()))
                        .map(ann -> (List<AnnotationValue>) ann.getElementValues().values().iterator().next().getValue())
                        .flatMap(List::stream)
                        .map(ann -> (AnnotationMirror) ann.getValue()))
                .toList();

        if (annotations.isEmpty()) {
            return null;
        }

        return annotations.stream()
                .map(annotation -> handleOne(annotation, element, generator))
                .distinct()
                .toList();

    }

    @SuppressWarnings("unchecked")
    private static List<TypeName> handleOne(AnnotationMirror annotation, Element element, AbstractGenerator generator) {
        if (annotation.getElementValues().isEmpty()) {
            generator.printError("@One must have atleast one component set. Remove the annotation or declare atleast one component.", element);
            return List.of();
        }

        var components = (List<AnnotationValue>) annotation.getElementValues().values().iterator().next().getValue();
        if (components.isEmpty()) {
            generator.printError("@One must have atleast two components set. Remove the annotation or declare atleast two components.", element);
            return List.of();
        }

        if (components.size() == 1) {
            generator.printError("@One must have atleast two components set. Use @All instead.", element);
            return List.of();
        }

        return components.stream()
                .map(AnnotationValue::getValue)
                .map(DeclaredType.class::cast)
                .flatMap(component -> processComponent(element, component, generator))
                .distinct()
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<TypeName> detectNone(Element element, AbstractGenerator generator) {
        var annotation = element.getAnnotationMirrors().stream()
                .filter(ann -> ann.getAnnotationType().toString().equals(Utils.NONE.canonicalName()))
                .findFirst()
                .orElse(null);

        if (annotation == null) {
            return null;
        }

        if (annotation.getElementValues().isEmpty()) {
            generator.printError("@None must have atleast one component set. Remove the annotation or declare atleast one component.");
            generator.printError("@None must have atleast one component set. Remove the annotation or declare atleast one component.", element);
            return List.of();
        }

        var components = (List<AnnotationValue>) annotation.getElementValues().values().iterator().next().getValue();
        if (components.isEmpty()) {
            generator.printError("@None must have atleast one component set. Remove the annotation or declare atleast one component.");
            generator.printError("@None must have atleast one component set. Remove the annotation or declare atleast one component.", element);
            return List.of();
        }

        return components.stream()
                .map(AnnotationValue::getValue)
                .map(DeclaredType.class::cast)
                .flatMap(component -> processComponent(element, component, generator))
                .distinct()
                .toList();
    }

    private static Stream<TypeName> processComponent(Element element, DeclaredType component, AbstractGenerator generator) {
        if (generator.components.getSelectedComponent(component) == null) {
            generator.printError("Type \"%s\" is not a component. Remove the type, or annotate it with @Component.".formatted(component));
            generator.printError("Type \"%s\" is not a component. Remove the type, or annotate it with @Component.".formatted(component), element);
            return Stream.empty();
        }

        return Stream.of(ClassName.get(component));
    }

}
