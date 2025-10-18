package de.schosin.decs.codegen.entityarchetype;

import com.palantir.javapoet.*;
import de.schosin.decs.codegen.components.ComponentData;
import de.schosin.decs.codegen.components.ComponentData.ClassComponent;
import de.schosin.decs.codegen.components.ComponentData.EnumComponent;
import de.schosin.decs.codegen.components.ComponentData.InterfaceComponent;
import de.schosin.decs.codegen.components.ComponentData.SingletonEnumComponent;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class EntityArchetypeDataGenerator extends AbstractGenerator {

    public record InterfaceComponentField(FieldSpec field, ArrayTypeName dataType,
                                          Function<String, CodeBlock> newInstance) {
    }

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
        return components.sortedComponents().stream()
                .flatMap(this::createComponentFields)
                .toList();
    }

    private Stream<FieldSpec> createComponentFields(ComponentData data) {
        return switch (data) {
            case ClassComponent component -> createClassComponentFields(component);
            case EnumComponent component -> createEnumComponentFields(component);
            case SingletonEnumComponent ignored -> Stream.empty();
            case InterfaceComponent component -> createInterfaceComponentFields(component);
        };
    }

    private Stream<FieldSpec> createClassComponentFields(ClassComponent data) {
        var bagType = ParameterizedTypeName.get(Utils.BAG, data.className());
        var bag = FieldSpec.builder(bagType, data.fieldName(), Modifier.PUBLIC, Modifier.FINAL).build();

        return Stream.of(bag);
    }

    private Stream<FieldSpec> createEnumComponentFields(EnumComponent data) {
        var type = ParameterizedTypeName.get(Utils.BAG, data.className());

        return Stream.of(FieldSpec.builder(type, data.fieldName(), Modifier.PUBLIC, Modifier.FINAL).build());
    }

    private Stream<FieldSpec> createInterfaceComponentFields(InterfaceComponent data) {
        return data.fields().stream()
                .map(field -> createInterfaceComponentField(data, field, this))
                .map(InterfaceComponentField::field);
    }

    public static InterfaceComponentField createInterfaceComponentField(InterfaceComponent data, Parameter field, AbstractGenerator generator) {
        var fieldName = data.fieldName() + "_" + field.name();

        if (!field.type().isPrimitive()) {
            var fieldSpec = FieldSpec.builder(ParameterizedTypeName.get(Utils.BAG, field.type()), fieldName, Modifier.PUBLIC, Modifier.FINAL).build();
            var dataType = ArrayTypeName.of(field.type());

            return new InterfaceComponentField(fieldSpec, dataType, entitySizeParam -> CodeBlock.builder().add("new $1T<>($3T.class, %s)".formatted(entitySizeParam), Utils.BAG, field.type()).build());
        }

        var bag = Utils.PRIMITIVE_BAGS.get(field.type().toString());
        var dataType = Utils.PRIMITIVE_BAG_DATA.get(field.type().toString());
        if (bag == null || dataType == null) {
            throw new IllegalStateException("Unknown primitive '%s'".formatted(field.type()));
        }

        var fieldSpec = FieldSpec.builder(bag, fieldName, Modifier.PUBLIC, Modifier.FINAL).build();
        return new InterfaceComponentField(fieldSpec, dataType, entitySizeParam -> CodeBlock.builder().add("new $1T(%s)".formatted(entitySizeParam), bag).build());
    }

    private MethodSpec factory() {
        var code = CodeBlock.builder();
        var arguments = CodeBlock.builder();

        code.addStatement("$1T[] data = new $1T[components.size()]", Utils.DATA_BAG);
        code.addStatement("int i = 0");

        for (var component : components.sortedComponents()) {
            switch (component) {
                case ClassComponent c -> {
                    code.add(System.lineSeparator());

                    code.addStatement("$1T<$2T> %s = components.contains($2T.class) ? new $1T<>($2T.class, entitySize) : null".formatted(c.fieldName()), Utils.BAG, c.className());

                    code.beginControlFlow("if (%s != null)".formatted(c.fieldName()));
                    code.addStatement("data[i++] = $1T.create(%s, ($2T<$3T>) $4T.getMetadata($3T.class).getPool())".formatted(c.fieldName()), Utils.DATA_BAG, Utils.POOL, c.className(), Utils.COMPONENTS);
                    code.endControlFlow();

                    arguments.add(", %s".formatted(c.fieldName()));
                }
                case EnumComponent c -> {
                    code.add(System.lineSeparator());
                    code.addStatement("$1T<$2T> %s = components.contains($2T.class) ? new $1T<>($2T.class, entitySize) : null".formatted(c.fieldName()), Utils.BAG, c.className());

                    code.beginControlFlow("if (%s != null)".formatted(c.fieldName()));
                    code.addStatement("data[i++] = $1T.create(%s)".formatted(c.fieldName()), Utils.DATA_BAG);
                    code.endControlFlow();

                    arguments.add(", %s".formatted(c.fieldName()));
                }
                case SingletonEnumComponent c -> {
                    code.add(System.lineSeparator());

                    code.beginControlFlow("if (components.contains($1T.class))", c.className());
                    code.addStatement("i++");
                    code.endControlFlow();
                }
                case InterfaceComponent c -> {
                    code.add(System.lineSeparator());

                    for (var field : c.fields()) {
                        var fieldSpec = createInterfaceComponentField(c, field, this);

                        code.addStatement("$1T %s = null".formatted(fieldSpec.field.name()), fieldSpec.field.type());
                        arguments.add(", %s".formatted(fieldSpec.field.name()));
                    }

                    code.beginControlFlow("if (components.contains($1T.class))", c.className());

                    var fieldBags = CodeBlock.builder();
                    for (int i = 0, s = c.fields().size(); i < s; i++) {
                        var field = c.fields().get(i);
                        var fieldSpec = createInterfaceComponentField(c, field, this);

                        code.add("%s = ".formatted(fieldSpec.field.name())).addStatement(fieldSpec.newInstance.apply("entitySize"));
                        fieldBags.add(i == 0 ? "" : ", ").add("$1T.create($2S, %s)".formatted(fieldSpec.field.name()), Utils.DATA_BAG, "%s#%s".formatted(c.className().simpleName(), field.name()));
                    }

                    code.add(System.lineSeparator());
                    code.add("data[i++] = $1T.create(", Utils.DATA_BAG).add(fieldBags.build()).addStatement(")");

                    code.endControlFlow();
                }
            }
        }

        code.add(System.lineSeparator());
        code.add("return new $1T(components, data", Utils.ENTITY_ARCHETYPE_DATA_IMPL);
        code.add(arguments.build());
        code.addStatement(")");

        return MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.INT, "entitySize")
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), Utils.CLASS_WILDCARD), "components")
                .returns(Utils.ENTITY_ARCHETYPE_DATA_IMPL)
                .addCode(code.build())
                .build();
    }

    private MethodSpec constructor() {
        var code = CodeBlock.builder();
        code.addStatement("super(data)");

        var parameters = new ArrayList<ParameterSpec>(components.sortedComponents().size());
        if (!components.sortedComponents().isEmpty()) {
            for (var component : components.sortedComponents()) {
                switch (component) {
                    case ClassComponent c -> {
                        parameters.add(ParameterSpec.builder(ParameterizedTypeName.get(Utils.BAG, c.className()), c.fieldName()).build());

                        code.add(System.lineSeparator());
                        code.addStatement("this.%1$s = %1$s".formatted(c.fieldName()));
                    }
                    case EnumComponent c -> {
                        parameters.add(ParameterSpec.builder(ParameterizedTypeName.get(Utils.BAG, c.className()), c.fieldName()).build());

                        code.add(System.lineSeparator());
                        code.addStatement("this.%1$s = %1$s".formatted(c.fieldName()));
                    }
                    case SingletonEnumComponent ignored -> {
                    }
                    case InterfaceComponent c -> {
                        code.add(System.lineSeparator());

                        for (var field : c.fields()) {
                            var fieldSpec = createInterfaceComponentField(c, field, this).field;
                            parameters.add(ParameterSpec.builder(fieldSpec.type(), fieldSpec.name()).build());

                            code.addStatement("this.%1$s = %1$s".formatted(fieldSpec.name()));
                        }
                    }
                }
            }
        }

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), Utils.CLASS_WILDCARD), "components")
                .addParameter(ArrayTypeName.of(Utils.DATA_BAG), "data")
                .addParameters(parameters)
                .addCode(code.build())
                .build();
    }

}

