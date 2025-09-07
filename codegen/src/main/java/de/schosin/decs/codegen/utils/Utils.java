package de.schosin.decs.codegen.utils;

import java.time.Instant;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;

import de.schosin.decs.codegen.DecsAnnotationProcessor;
import de.schosin.decs.codegen.system.CompositionData;

public class Utils {

    public static final AnnotationSpec GENERATED = AnnotationSpec.builder(ClassName.get("de.schosin.decs.api.annotations", "Generated"))
            .addMember("value", "\"%s\"".formatted(DecsAnnotationProcessor.class.getName()))
            .addMember("date", "\"%s\"".formatted(Instant.now()))
            .build();;

    public static final ClassName WORLD = ClassName.get("de.schosin.decs.api", "World");
    public static final ClassName INTERNAL_WORLD = ClassName.get("de.schosin.decs.api.internal", "InternalWorld");

    public static final ClassName UTILITY = ClassName.get("de.schosin.decs.api.annotations", "Utility");
    public static final ClassName SINGLETON = ClassName.get("de.schosin.decs.api.annotations", "Singleton");
    public static final ClassName VALUE = ClassName.get("de.schosin.decs.api.annotations", "Value");

    public static final ClassName ALL = ClassName.get("de.schosin.decs.api.annotations.composition", "All");
    public static final ClassName ONE = ClassName.get("de.schosin.decs.api.annotations.composition", "One");
    public static final ClassName ONES = ClassName.get("de.schosin.decs.api.annotations.composition", "Ones");
    public static final ClassName NONE = ClassName.get("de.schosin.decs.api.annotations.composition", "None");

    public static final ClassName SYSTEM_TYPE = ClassName.get("de.schosin.decs.api.systems", "SystemType");
    public static final ClassName UTILITY_TYPE = ClassName.get("de.schosin.decs.api.systems", "UtilityType");

    public static final ClassName ENTITY = ClassName.get("de.schosin.decs.api.entities", "Entity");
    public static final ClassName ENTITY_REF = ClassName.get("de.schosin.decs.api.entities", "EntityRef");
    public static final ClassName INTERNAL_ENTITY = ClassName.get("de.schosin.decs.api.internal", "InternalEntity");

    public static final ClassName ENTITY_ARCHETYPE = ClassName.get("de.schosin.decs.api.entities", "EntityArchetype");
    public static final ClassName ENTITY_ARCHETYPE_LISTENER = ClassName.get("de.schosin.decs.api.entities", "EntityArchetypeListener");
    public static final ClassName COMPOSITION = ClassName.get("de.schosin.decs.api.entities", "Composition");
    public static final ClassName TRANSMUTATION = ClassName.get("de.schosin.decs.api.entities", "Transmutation");
    public static final ClassName TRANSITION = ClassName.get("de.schosin.decs.api.entities", "Transition");

    public static final ClassName BAG = ClassName.get("de.schosin.decs.api.utils.collections", "Bag");
    public static final ClassName INT_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "IntBag");
    public static final ClassName ENTITY_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "EntityBag");

    public static final ClassName POOL = ClassName.get("de.schosin.decs.api.utils.pool", "Pool");

    public static final ClassName VALUES = ClassName.get("de.schosin.decs.values", "Values");

    public static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public static String decapitalize(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    public static String toUpperSnakeCase(String value) {
        return value
                .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
                .replaceAll("([A-Z\\d])([A-Z][a-z])", "$1_$2")
                .toUpperCase();
    }

    public static <T, R> Predicate<T> distinctBy(Function<T, R> accessor) {
        var seen = new HashSet<R>();
        return item -> seen.add(accessor.apply(item));
    }

    public static FieldSpec buildComposition(CompositionData composition) {
        return buildComposition(composition, "COMPOSITION");
    }

    public static FieldSpec buildComposition(CompositionData composition, String name) {
        if (composition.isAllEntities()) {
            return FieldSpec.builder(COMPOSITION, name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$1T.all().build()", COMPOSITION)
                    .build();
        }

        var code = CodeBlock.builder();
        code.add("$1T", COMPOSITION);

        if (composition.all() != null) {
            code.add(System.lineSeparator()).indent().add(".all(");

            var types = composition.all();
            for (int i = 0, s = types.size(); i < s; i++) {
                if (i > 0) {
                    code.add(", ");
                }

                code.add("$1T.class", types.get(i));
            }

            code.unindent().add(")");
        }

        if (composition.ones() != null) {
            for (var types : composition.ones()) {
                code.add(System.lineSeparator()).indent().add(".one(");

                for (int i = 0, s = types.size(); i < s; i++) {
                    if (i > 0) {
                        code.add(", ");
                    }

                    code.add("$1T.class", types.get(i));
                }

                code.unindent().add(")");
            }
        }
        if (composition.none() != null) {
            code.add(System.lineSeparator()).indent().add(".none(");

            var types = composition.none();
            for (int i = 0, s = types.size(); i < s; i++) {
                if (i > 0) {
                    code.add(", ");
                }

                code.add("$1T.class", types.get(i));
            }

            code.unindent().add(")");
        }

        code.add(System.lineSeparator()).indent().add(".build()").unindent();

        return FieldSpec.builder(COMPOSITION, name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(code.build())
                .build();
    }

    public static Annotations getAnnotations(ProcessingEnvironment processingEnv) {
        return new Annotations(processingEnv.getElementUtils());
    }

}
