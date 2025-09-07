package de.schosin.decs.codegen.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

abstract class AbstractUtilityTest {

    protected final String base;
    protected final String annotation;

    protected AbstractUtilityTest(String base, String annotation) {
        this.base = base;
        this.annotation = annotation;
    }

    @ParameterizedTest
    @CsvSource({
            "EnumTypeSystem,7,8", "RecordTypeSystem,7,8"
    })
    void testErrorOnInvalidFinalType(String type, int line, int column) {
        var source = this.base + "/invalidtype/%s.java".formatted(type);

        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles(source)
                .compilationFails()
                .source(source, it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(line, column, "Types using @%s must be an abstract class.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnInterfaceType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles(this.base + "/invalidtype/InterfaceTypeSystem.java")
                .compilationFails()
                .source(this.base + "/invalidtype/InterfaceTypeSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(7, 8, "Types using @%s must be an abstract class.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnAnnotationType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles(this.base + "/invalidtype/AnnotationTypeSystem.java")
                .compilationFails()
                .source(this.base + "/invalidtype/AnnotationTypeSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(5, 9, "Types using @%s must be an abstract class.".formatted(this.annotation)))
                .executeTest();
    }

}
