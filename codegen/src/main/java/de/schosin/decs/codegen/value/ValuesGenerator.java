package de.schosin.decs.codegen.value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.StandardLocation;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.utils.AbstractGenerator;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.utils.ParsedType;
import de.schosin.decs.codegen.utils.Utils;

public class ValuesGenerator extends AbstractGenerator {

    private static final String VALUE_TYPE_PRIMITIVE = "PRIMITIVE";
    private static final String VALUE_TYPE_CLASS = "CLASS";

    private static final String VALUES_PACKAGE = "de.schosin.decs.values";
    private static final String VALUES_NAME = "Values";

    private final ValuesResult values;

    public ValuesGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ComponentsResult components, ValuesResult values) {
        super(processingEnv, roundEnv, components);

        this.values = values;
    }

    public void process() {
        // Process @Value parameters
        roundEnv.getElementsAnnotatedWith(annotations.value()).stream()
                .filter(VariableElement.class::isInstance)
                .map(VariableElement.class::cast)
                .forEach(this::process);

        // Process meta-annotations
        var metaAnnotations = roundEnv.getElementsAnnotatedWith(annotations.value()).stream()
                .filter(TypeElement.class::isInstance)
                .map(TypeElement.class::cast)
                .collect(Collectors.toSet());

        // Process meta-annotation parameters
        for (var metaAnnotation : metaAnnotations) {
            roundEnv.getElementsAnnotatedWith(metaAnnotation).stream()
                    .filter(VariableElement.class::isInstance)
                    .map(VariableElement.class::cast)
                    .forEach(this::process);
        }
    }

    private void process(VariableElement variable) {
        var value = resolveAnnotation(variable, Utils.VALUE);

        var name = (String) value.getElementValues().values().iterator().next().getValue();
        if (SourceVersion.isKeyword(name)) {
            printError("@Value cannot use java language keyword: %s".formatted(name), variable);
            return;
        }

        if (variable.asType() instanceof DeclaredType declared && declared.getEnclosingType().getKind() != TypeKind.NONE) {
            printError("@Value cannot be used on nested type '%s'".formatted(variable.asType()), variable);
            return;
        }

        var data = new ValueData(name, TypeName.get(variable.asType()));

        values.values().merge(name, data, (first, second) -> {
            if (!first.type().equals(second.type())) {
                printError("@Value with name '%s' used with different types: %s / %s".formatted(first.name(), first.type(), second.type()), variable);
            }

            return second;
        });
    }

    private FieldSpec createField(ValueData value) {
        return FieldSpec.builder(value.type(), value.name(), Modifier.PUBLIC).build();
    }

    public JavaType generate() {
        var values = Manifest.getValues(this);

        var fields = values.stream()
                .map(this::createField)
                .toList();

        var type = TypeSpec.classBuilder(VALUES_NAME)
                .addAnnotation(Utils.GENERATED)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .build();

        return JavaType.create(VALUES_PACKAGE, type);
    }

    private static class Manifest {

        private static final String MANIFEST = "META-INF/decs/values/manifest";

        public static List<ValueData> getValues(ValuesGenerator generator) {
            // Read manifest
            var values = readManifest(generator);

            // Remove values in current round
            values.removeIf(existing -> generator.values.values().values().stream().anyMatch(value -> existing.name().equals(value.name())));

            // Add values from current round
            values.addAll(generator.values.values().values()); // beauty, isn't it?

            // Write updated manifest
            writeManifest(values, generator);

            return values;
        }

        private static List<ValueData> readManifest(ValuesGenerator generator) {
            var filer = generator.processingEnv.getFiler();
            var result = new ArrayList<ValueData>();

            try {
                var file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", MANIFEST);

                try (var reader = new BufferedReader(new InputStreamReader(file.openInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }

                        var split = line.split(":");
                        if (split.length != 3) {
                            generator.printError("Failed to read value metadata: Invalid format ('%s'), do a full rebuild.".formatted(line));
                            continue;
                        }

                        var name = split[0];
                        var type = split[1];
                        var typeName = split[2];

                        switch (type) {
                            case VALUE_TYPE_PRIMITIVE -> {
                                switch (typeName) {
                                    case "boolean" -> result.add(new ValueData(name, TypeName.BOOLEAN));
                                    case "byte" -> result.add(new ValueData(name, TypeName.BYTE));
                                    case "short" -> result.add(new ValueData(name, TypeName.SHORT));
                                    case "int" -> result.add(new ValueData(name, TypeName.INT));
                                    case "long" -> result.add(new ValueData(name, TypeName.LONG));
                                    case "char" -> result.add(new ValueData(name, TypeName.CHAR));
                                    case "float" -> result.add(new ValueData(name, TypeName.FLOAT));
                                    case "double" -> result.add(new ValueData(name, TypeName.DOUBLE));
                                    default -> {
                                        generator.printError("Failed to read value metadata for '%s': Invalid type (%s), do a full rebuild.".formatted(name, type));
                                    }
                                }
                            }
                            case VALUE_TYPE_CLASS -> {
                                result.add(new ValueData(name, ParsedType.parse(typeName).getTypeName()));
                            }
                            default -> {
                                generator.printError("Failed to read value metadata for '%s': Unknown type '%s' ('%s'), do a full rebuild.".formatted(name, type, line));
                            }
                        }
                    }
                }

                return result;
            } catch (NoSuchFileException ex) {
                return result;
            } catch (IOException ex) {
                // Eclipse JDT does not throw NoSuchFileException
                if (ex.getMessage().contains("does not exist")) {
                    return result;
                }

                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read values metadata: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
                return result;
            }
        }

        private static void writeManifest(List<ValueData> values, ValuesGenerator generator) {
            var filer = generator.processingEnv.getFiler();

            try {
                var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", MANIFEST);

                try (var writer = file.openWriter()) {
                    for (var value : values) {
                        writer.append(value.name());

                        if (value.type().isPrimitive()) {
                            writer.append(":").append(VALUE_TYPE_PRIMITIVE);
                            writer.append(":").append(value.type().toString());
                        } else {
                            writer.append(":").append(VALUE_TYPE_CLASS);
                            writer.append(":").append(value.type().toString());
                        }

                        writer.append(System.lineSeparator());
                    }
                }
            } catch (IOException ex) {
                var writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));

                generator.printError("Failed to read components metadata: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
            }
        }

    }

}
