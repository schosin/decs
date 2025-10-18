package de.schosin.decs.codegen.utils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

import com.palantir.javapoet.*;

import de.schosin.decs.codegen.DecsAnnotationProcessor;
import de.schosin.decs.codegen.system.CompositionData;

public class Utils {

    public static final AnnotationSpec GENERATED = AnnotationSpec.builder(ClassName.get("de.schosin.decs.api.annotations", "Generated"))
            .addMember("value", "\"%s\"".formatted(DecsAnnotationProcessor.class.getName()))
            .addMember("date", "\"%s\"".formatted(Instant.now()))
            .build();

    public static final ClassName WORLD = ClassName.get("de.schosin.decs.api", "World");

    public static final ClassName UTILITY = ClassName.get("de.schosin.decs.api.annotations", "Utility");
    public static final ClassName SINGLETON = ClassName.get("de.schosin.decs.api.annotations", "Singleton");
    public static final ClassName VALUE = ClassName.get("de.schosin.decs.api.annotations", "Value");

    public static final ClassName ALL = ClassName.get("de.schosin.decs.api.annotations.composition", "All");
    public static final ClassName ONE = ClassName.get("de.schosin.decs.api.annotations.composition", "One");
    public static final ClassName ONES = ClassName.get("de.schosin.decs.api.annotations.composition", "Ones");
    public static final ClassName NONE = ClassName.get("de.schosin.decs.api.annotations.composition", "None");

    public static final ClassName SYSTEM_PROCESSOR = ClassName.get("de.schosin.decs.api.annotations.system", "SystemProcessor");

    public static final ClassName INLINE = ClassName.get("de.schosin.decs.api.annotations.experimental", "Inline");

    public static final ClassName SYSTEM_TYPE = ClassName.get("de.schosin.decs.api.systems", "SystemType");
    public static final ClassName UTILITY_TYPE = ClassName.get("de.schosin.decs.api.systems", "UtilityType");

    public static final ClassName BASE_ENTITY = ClassName.get("de.schosin.decs.api.entities", "BaseEntity");
    public static final ClassName ENTITY = ClassName.get("de.schosin.decs.api.entities", "Entity");
    public static final ClassName ENTITY_REF = ClassName.get("de.schosin.decs.api.entities", "EntityRef");
    public static final ClassName ENTITY_ARCHETYPE = ClassName.get("de.schosin.decs.api.entities", "EntityArchetype");
    public static final ClassName ENTITY_ARCHETYPE_LISTENER = ClassName.get("de.schosin.decs.api.entities", "EntityArchetypeListener");
    public static final ClassName COMPOSITION = ClassName.get("de.schosin.decs.api.entities", "Composition");
    public static final ClassName TRANSMUTATION = ClassName.get("de.schosin.decs.api.entities", "Transmutation");
    public static final ClassName TRANSITION = ClassName.get("de.schosin.decs.api.entities", "Transition");

    public static final ClassName INTERNAL_WORLD = ClassName.get("de.schosin.decs.api.internal", "InternalWorld");
    public static final ClassName INTERNAL_BASE_ENTITY = ClassName.get("de.schosin.decs.api.internal", "InternalBaseEntity");
    public static final ClassName INTERNAL_ENTITY = ClassName.get("de.schosin.decs.api.internal", "InternalEntity");
    public static final ClassName ENTITY_ARCHETYPE_DATA = ClassName.get("de.schosin.decs.api.internal", "EntityArchetypeData");
    public static final ClassName ENTITY_ARCHETYPE_DATA_IMPL = ClassName.get("de.schosin.decs.api.internal", "EntityArchetypeDataImpl");

    public static final ClassName DATA_BAG = ClassName.get("de.schosin.decs.api.internal", "DataBag");

    public static final ClassName BAG = ClassName.get("de.schosin.decs.api.utils.collections", "Bag");
    public static final ClassName ENTITY_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "EntityBag");

    public static final ClassName BOOLEAN_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "BOOLEAN_BAG");
    public static final ClassName BYTE_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "ByteBag");
    public static final ClassName SHORT_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "ShortBag");
    public static final ClassName INT_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "IntBag");
    public static final ClassName LONG_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "LongBag");
    public static final ClassName CHAR_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "CharBag");
    public static final ClassName FLOAT_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "FloatBag");
    public static final ClassName DOUBLE_BAG = ClassName.get("de.schosin.decs.api.utils.collections", "DoubleBag");

    public static final ClassName POOL = ClassName.get("de.schosin.decs.api.utils.pool", "Pool");
    public static final ClassName POOLED = ClassName.get("de.schosin.decs.api.utils.pool", "Pooled");

    public static final ClassName COMPONENTS = ClassName.get("de.schosin.decs.values", "Components");
    public static final ClassName COMPONENT_METADATA = ClassName.get("de.schosin.decs.values", "ComponentMetadata");
    public static final ClassName VALUES = ClassName.get("de.schosin.decs.values", "Values");

    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(Object.class);
    public static final ParameterizedTypeName CLASS_WILDCARD = ParameterizedTypeName.get(ClassName.get(Class.class), WILDCARD);

    public static final Map<String, ClassName> PRIMITIVE_BAGS = Map.of(
            "boolean", BOOLEAN_BAG,
            "byte", BYTE_BAG,
            "short", SHORT_BAG,
            "int", INT_BAG,
            "long", LONG_BAG,
            "char", CHAR_BAG,
            "float", FLOAT_BAG,
            "double", DOUBLE_BAG
    );

    public static final Map<String, ArrayTypeName> PRIMITIVE_BAG_DATA = Map.of(
            "boolean", ArrayTypeName.of(TypeName.BOOLEAN),
            "byte", ArrayTypeName.of(TypeName.BYTE),
            "short", ArrayTypeName.of(TypeName.SHORT),
            "int", ArrayTypeName.of(TypeName.INT),
            "long", ArrayTypeName.of(TypeName.LONG),
            "char", ArrayTypeName.of(TypeName.CHAR),
            "float", ArrayTypeName.of(TypeName.FLOAT),
            "double", ArrayTypeName.of(TypeName.DOUBLE)
    );

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
