package de.schosin.decs.codegen;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.JavaFile;
import de.schosin.decs.codegen.components.ComponentsGenerator;
import de.schosin.decs.codegen.components.ComponentsResult;
import de.schosin.decs.codegen.entityarchetype.EntityArchetypeDataGenerator;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.SystemsResult;
import de.schosin.decs.codegen.utils.Annotations;
import de.schosin.decs.codegen.utils.JavaType;
import de.schosin.decs.codegen.value.ValuesGenerator;
import de.schosin.decs.codegen.value.ValuesResult;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        Annotations.VALUE,
        Annotations.COMPONENT,
        Annotations.COUNT, Annotations.ARCHETYPE, Annotations.TRANSMUTE,
        Annotations.ALL, Annotations.ONE, Annotations.ONES, Annotations.NONE,
        Annotations.SYSTEM_PROCESSOR, Annotations.ENTITY_PROCESSOR, Annotations.INSERTED, Annotations.REMOVED,
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class DecsAnnotationProcessor extends AbstractProcessor {

    private final ComponentsResult components = new ComponentsResult();
    private final ValuesResult values = new ValuesResult();
    private final SystemsResult systems = new SystemsResult();

    private int round;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var buildEnv = BuildEnvironment.determine(processingEnv);
        processingEnv.getMessager().printWarning("Round %d (over %b, env: %s): %s".formatted(++round, roundEnv.processingOver(), buildEnv, roundEnv.getRootElements()));

        if (roundEnv.errorRaised()) {
            return false;
        }

        // Get @Component types
        var componentsGenerator = new ComponentsGenerator(processingEnv, roundEnv, components);
        componentsGenerator.process();

        if (componentsGenerator.isError()) {
            return false;
        }

        // Generate Values
        var valuesGenerator = new ValuesGenerator(processingEnv, roundEnv, components, values);
        valuesGenerator.process();

        if (valuesGenerator.isError()) {
            return false;
        }

        // Generate system types
        var systemGenerator = new SystemGenerator(processingEnv, roundEnv, components, systems);
        systemGenerator.process();

        if (systemGenerator.isError()) {
            return false;
        }

        // Create files in last round
        if (roundEnv.processingOver() || buildEnv.generateFirstRound()) {
            var valuesType = valuesGenerator.generate();
            writeFile(valuesType);

            var components = componentsGenerator.generate();
            writeFiles(components);

            var entityArchetypeDataGenerator = new EntityArchetypeDataGenerator(processingEnv, roundEnv, this.components);
            var entityArchetypeData = entityArchetypeDataGenerator.generate();
            writeFile(entityArchetypeData);

            var systems = systemGenerator.generate();
            writeFiles(systems);
        }

        return true;
    }

    private void writeFiles(List<JavaType> javaTypes) {
        for (var type : javaTypes) {
            writeFile(type);
        }
    }

    private void writeFile(JavaType type) {
        var file = JavaFile.builder(type.packageName(), type.type())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();

        try {
            file.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write generated file: " + e.getMessage());
        }
    }

}
