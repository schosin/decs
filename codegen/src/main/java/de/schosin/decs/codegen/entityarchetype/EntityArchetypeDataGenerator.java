package de.schosin.decs.codegen.entityarchetype;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentData;
import de.schosin.decs.codegen.components.ComponentData.ClassComponent;
import de.schosin.decs.codegen.components.ComponentData.EnumComponent;
import de.schosin.decs.codegen.components.ComponentData.SingletonEnumComponent;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class EntityArchetypeDataGenerator extends AbstractGenerator {

    public EntityArchetypeDataGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components) {
        super(processingEnv, roundEnv, components);
    }

    public JavaType generate() {
        var type = TypeSpec.classBuilder(Utils.ENTITY_ARCHETYPE_DATA_IMPL)
                .addAnnotation(Utils.GENERATED)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(Utils.ENTITY_ARCHETYPE_DATA)
                .addFields(componentFields())
                .addMethod(factory())
                .addMethod(constructor())
                .build();

        return JavaType.create(Utils.ENTITY_ARCHETYPE_DATA_IMPL.packageName(), type);
    }

    private List<FieldSpec> componentFields() {
        return components.components().values().stream()
                .flatMap(this::createComponentFields)
                .toList();
    }

    private Stream<FieldSpec> createComponentFields(ComponentData data) {
        return switch (data) {
            case ClassComponent component -> createClassComponentFields(component);
            case EnumComponent component -> createEnumComponentFields(component);
            case SingletonEnumComponent ignored -> Stream.empty();
        };
    }

    private Stream<FieldSpec> createClassComponentFields(ComponentData data) {
        var bagType = ParameterizedTypeName.get(Utils.BAG, data.className());
        var bag = FieldSpec.builder(bagType, data.fieldName(), Modifier.PUBLIC, Modifier.FINAL).build();

        var poolType = ParameterizedTypeName.get(Utils.POOL, data.className());
        var pool = FieldSpec.builder(poolType, data.poolFieldName(), Modifier.PRIVATE, Modifier.FINAL).build();

        return Stream.of(bag, pool);
    }

    private Stream<FieldSpec> createEnumComponentFields(ComponentData data) {
        var type = ParameterizedTypeName.get(Utils.BAG, data.className());

        return Stream.of(FieldSpec.builder(type, data.fieldName(), Modifier.PUBLIC, Modifier.FINAL).build());
    }

    private MethodSpec factory() {
        var code = CodeBlock.builder();
        var arguments = CodeBlock.builder();

        code.addStatement("$1T<$2T>[] data = new $1T[components.size()]", Utils.BAG, ClassName.OBJECT);
        code.addStatement("$1T<$2T>[] pools = new $1T[components.size()]", Utils.POOL, ClassName.OBJECT);
        code.addStatement("int i = 0");

        for (var component : components.sortedComponents()) {
            switch (component) {
                case ClassComponent c -> {
                    code.add(System.lineSeparator());

                    code.addStatement("$1T<$2T> %s = components.contains($2T.class) ? new $1T<>($2T.class, entitySize) : null".formatted(c.fieldName()), Utils.BAG, c.className());
                    code.addStatement("$1T<$2T> %s = %s != null ? ($1T<$2T>) $3T.getMetadata($2T.class).getPool() : null".formatted(c.poolFieldName(), c.fieldName()), Utils.POOL, c.className(), Utils.COMPONENTS);

                    code.beginControlFlow("if (%s != null)".formatted(c.fieldName()));
                    code.addStatement("data[i] = ($1T) %s".formatted(c.fieldName()), Utils.BAG);
                    code.addStatement("pools[i++] = ($1T) %s".formatted(c.poolFieldName()), Utils.POOL);
                    code.endControlFlow();

                    arguments.add(", %s, %s".formatted(c.fieldName(), c.poolFieldName()));
                }
                case EnumComponent c -> {
                    code.add(System.lineSeparator());
                    code.addStatement("$1T<$2T> %s = components.contains($2T.class) ? new $1T<>($2T.class, entitySize) : null".formatted(c.fieldName()), Utils.BAG, c.className());

                    code.beginControlFlow("if (%s != null)".formatted(c.fieldName()));
                    code.addStatement("data[i++] = ($1T) %s".formatted(c.fieldName()), Utils.BAG);
                    code.endControlFlow();

                    arguments.add(", %s".formatted(c.fieldName()));
                }
                case SingletonEnumComponent c -> {
                    code.add(System.lineSeparator());

                    code.beginControlFlow("if (components.contains($1T.class))", c.className());
                    code.addStatement("i++");
                    code.endControlFlow();
                }
            }
        }

        code.add(System.lineSeparator());
        code.add("return new $1T(components, data, pools", Utils.ENTITY_ARCHETYPE_DATA_IMPL);
        code.add(arguments.build());
        code.addStatement(")");

        return MethodSpec.methodBuilder("create")
                .addAnnotation(Utils.SUPPRESS_UNCHECKED_RAWTYPES)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.INT, "entitySize")
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), Utils.CLASS_WILDCARD), "components")
                .returns(Utils.ENTITY_ARCHETYPE_DATA_IMPL)
                .addCode(code.build())
                .build();
    }

    private MethodSpec constructor() {
        var code = CodeBlock.builder();
        code.addStatement("super(data, pools)");

        var parameters = new ArrayList<ParameterSpec>(components.sortedComponents().size());
        if (!components.sortedComponents().isEmpty()) {
            for (var component : components.sortedComponents()) {
                switch (component) {
                    case ClassComponent c -> {
                        parameters.add(ParameterSpec.builder(ParameterizedTypeName.get(Utils.BAG, c.className()), c.fieldName()).build());
                        parameters.add(ParameterSpec.builder(ParameterizedTypeName.get(Utils.POOL, c.className()), c.poolFieldName()).build());

                        code.add(System.lineSeparator());
                        code.addStatement("this.%1$s = %1$s".formatted(c.fieldName()));
                        code.addStatement("this.%1$s = %1$s".formatted(c.poolFieldName()));
                    }
                    case EnumComponent c -> {
                        parameters.add(ParameterSpec.builder(ParameterizedTypeName.get(Utils.BAG, c.className()), c.fieldName()).build());

                        code.add(System.lineSeparator());
                        code.addStatement("this.%1$s = %1$s".formatted(c.fieldName()));
                    }
                    case SingletonEnumComponent ignored -> {
                    }
                }
            }
        }

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), Utils.CLASS_WILDCARD), "components")
                .addParameter(ArrayTypeName.of(ParameterizedTypeName.get(Utils.BAG, ClassName.OBJECT)), "data")
                .addParameter(ArrayTypeName.of(ParameterizedTypeName.get(Utils.POOL, ClassName.OBJECT)), "pools")
                .addParameters(parameters)
                .addCode(code.build())
                .build();
    }

}

