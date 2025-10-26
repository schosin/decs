package de.schosin.decs.codegen.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

public record Annotations(
        TypeElement systems,
        TypeElement utility, TypeElement singleton, TypeElement value,
        TypeElement component,
        TypeElement count, TypeElement archetype, TypeElement transmute, TypeElement entityInitializer,
        TypeElement all, TypeElement one, TypeElement ones, TypeElement none,
        TypeElement systemProcessor, TypeElement entityProcessor, TypeElement inserted, TypeElement removed,
        DeclaredType systemsType,
        DeclaredType utilityType, DeclaredType singletonType, DeclaredType valueType,
        DeclaredType componentType,
        DeclaredType countType, DeclaredType archetypeType, DeclaredType transmuteType,
        DeclaredType entityInitializerType,
        DeclaredType allType, DeclaredType oneType, DeclaredType onesType, DeclaredType noneType,
        DeclaredType systemProcessorType, DeclaredType entityProcessorType, DeclaredType insertedType,
        DeclaredType removedType) {


    public static final String SYSTEMS = "de.schosin.decs.api.annotations.invocation.Systems";

    public static final String UTILITY = "de.schosin.decs.api.annotations.Utility";
    public static final String SINGLETON = "de.schosin.decs.api.annotations.Singleton";
    public static final String VALUE = "de.schosin.decs.api.annotations.Value";

    public static final String COMPONENT = "de.schosin.decs.api.annotations.components.Component";

    public static final String COUNT = "de.schosin.decs.api.annotations.utils.Count";
    public static final String ARCHETYPE = "de.schosin.decs.api.annotations.utils.Archetype";
    public static final String TRANSMUTE = "de.schosin.decs.api.annotations.utils.Transmute";
    public static final String ENTITY_INITIALIZER = "de.schosin.decs.api.annotations.utils.EntityInitializer";

    public static final String ALL = "de.schosin.decs.api.annotations.composition.All";
    public static final String ONE = "de.schosin.decs.api.annotations.composition.One";
    public static final String ONES = "de.schosin.decs.api.annotations.composition.Ones";
    public static final String NONE = "de.schosin.decs.api.annotations.composition.None";

    public static final String SYSTEM_PROCESSOR = "de.schosin.decs.api.annotations.system.SystemProcessor";
    public static final String ENTITY_PROCESSOR = "de.schosin.decs.api.annotations.system.EntityProcessor";
    public static final String INSERTED = "de.schosin.decs.api.annotations.system.Inserted";
    public static final String REMOVED = "de.schosin.decs.api.annotations.system.Removed";

    public Annotations(Elements elements) {
        this(
                elements.getTypeElement(SYSTEMS),
                elements.getTypeElement(UTILITY),
                elements.getTypeElement(SINGLETON),
                elements.getTypeElement(VALUE),
                elements.getTypeElement(COMPONENT),
                elements.getTypeElement(COUNT),
                elements.getTypeElement(ARCHETYPE),
                elements.getTypeElement(TRANSMUTE),
                elements.getTypeElement(ENTITY_INITIALIZER),
                elements.getTypeElement(ALL),
                elements.getTypeElement(ONE),
                elements.getTypeElement(ONES),
                elements.getTypeElement(NONE),
                elements.getTypeElement(SYSTEM_PROCESSOR),
                elements.getTypeElement(ENTITY_PROCESSOR),
                elements.getTypeElement(INSERTED),
                elements.getTypeElement(REMOVED),

                (DeclaredType) elements.getTypeElement(SYSTEMS).asType(),
                (DeclaredType) elements.getTypeElement(UTILITY).asType(),
                (DeclaredType) elements.getTypeElement(SINGLETON).asType(),
                (DeclaredType) elements.getTypeElement(VALUE).asType(),
                (DeclaredType) elements.getTypeElement(COMPONENT).asType(),
                (DeclaredType) elements.getTypeElement(COUNT).asType(),
                (DeclaredType) elements.getTypeElement(ARCHETYPE).asType(),
                (DeclaredType) elements.getTypeElement(TRANSMUTE).asType(),
                (DeclaredType) elements.getTypeElement(ENTITY_INITIALIZER).asType(),
                (DeclaredType) elements.getTypeElement(ALL).asType(),
                (DeclaredType) elements.getTypeElement(ONE).asType(),
                (DeclaredType) elements.getTypeElement(ONES).asType(),
                (DeclaredType) elements.getTypeElement(NONE).asType(),
                (DeclaredType) elements.getTypeElement(SYSTEM_PROCESSOR).asType(),
                (DeclaredType) elements.getTypeElement(ENTITY_PROCESSOR).asType(),
                (DeclaredType) elements.getTypeElement(INSERTED).asType(),
                (DeclaredType) elements.getTypeElement(REMOVED).asType());

        validate();
    }

    public Set<DeclaredType> getSystemMethodAnnotations() {
        return Set.of(systemProcessorType, entityProcessorType, insertedType, removedType);
    }

    public Set<DeclaredType> getUtilityMethodAnnotations() {
        return Set.of(countType, archetypeType, transmuteType);
    }

    private void validate() {
        // sanity check
        var components = Annotations.class.getRecordComponents();
        for (int i = 0, s = components.length; i < s; i++) {
            var field = components[i];

            var name = TypeElement.class.equals(field.getType())
                    ? field.getName()
                    : field.getName().substring(0, field.getName().length() - 4);

            var annotation = name.substring(0, 1).toUpperCase() + name.substring(1);

            try {
                switch (field.getAccessor().invoke(this)) {
                    case TypeElement element when !element.getSimpleName().contentEquals(annotation) ->
                            throw new IllegalStateException(
                                    "Element '%s' at position %d not setup properly: Expected '%s', but was '%s'".formatted(field.getName(), i, element.getSimpleName(), annotation));
                    case DeclaredType type when !type.asElement().getSimpleName().contentEquals(annotation) ->
                            throw new IllegalStateException(
                                    "Type '%s' at position %d not setup properly: Expected '%s', but was '%s'".formatted(field.getName(), i, type.asElement().getSimpleName(), annotation));
                    default -> {
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("Annotation '%s' (position %d) not setup properly: %s".formatted(name, i, ex.getMessage()));
            }
        }
    }
}
