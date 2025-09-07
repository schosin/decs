package de.schosin.decs.codegen.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

import spoon.reflect.declaration.CtType;

class CommonProcessorTest {

    @Test
    void testSuccessWhenCompositionOnType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/CompositionOnTypeSystem.java")
                .compilationSucceeds()
                .source("/system/common/CompositionOnTypeSystem.java", it -> it.hasNoWarnings())
                .generatedClass("foo.CompositionOnTypeSystemImpl", type -> {
                    assertThat(type.getNestedTypes())
                            .extracting(CtType::getSimpleName)
                            .containsExactlyInAnyOrder("InsertedInsertedHandler", "ProcessEntity", "RemovedRemovedHandler");
                })
                .executeTest();
    }

    @Test
    void testErrorOnMissingComposition() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/MissingCompositionSystem.java")
                .compilationFails()
                .source("/system/common/MissingCompositionSystem.java", it -> it
                        .hasErrors(3).hasWarnings(0)
                        .hasErrorContaining(10, 16, "@Inserted method or its owning class must be annotated with @All/@One/@None.")
                        .hasErrorContaining(14, 16, "@EntityProcessor method or its owning class must be annotated with @All/@One/@None.")
                        .hasErrorContaining(18, 16, "@Removed method or its owning class must be annotated with @All/@One/@None."))
                .executeTest();
    }

    @Test
    void testErrorOnNonVoidReturnType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/NonVoidReturnSystem.java")
                .compilationFails()
                .source("/system/common/NonVoidReturnSystem.java", it -> it
                        .hasErrors(4).hasWarnings(0)
                        .hasErrorContaining(13, 15, "Methods annotated with @Inserted must return void.")
                        .hasErrorContaining(19, 15, "Methods annotated with @EntityProcessor must return void.")
                        .hasErrorContaining(25, 15, "Methods annotated with @Removed must return void.")
                        .hasErrorContaining(30, 15, "Methods annotated with @SystemProcessor must return void."))
                .executeTest();
    }

    @Test
    void testErrorOnPrivateMethod() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/PrivateMethodSystem.java")
                .compilationFails()
                .source("/system/common/PrivateMethodSystem.java", it -> it
                        .hasErrors(4).hasWarnings(0)
                        .hasErrorContaining(13, 24, "Methods annotated with @Inserted must not be private.")
                        .hasErrorContaining(18, 24, "Methods annotated with @EntityProcessor must not be private.")
                        .hasErrorContaining(23, 24, "Methods annotated with @Removed must not be private.")
                        .hasErrorContaining(27, 24, "Methods annotated with @SystemProcessor must not be private."))
                .executeTest();
    }

    @Test
    void testErrorOnNonFinalMethod() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/NonFinalMethodSystem.java")
                .compilationFails()
                .source("/system/common/NonFinalMethodSystem.java", it -> it
                        .hasErrors(4).hasWarnings(0)
                        .hasErrorContaining(13, 10, "Methods annotated with @Inserted must be final.")
                        .hasErrorContaining(18, 10, "Methods annotated with @EntityProcessor must be final.")
                        .hasErrorContaining(23, 10, "Methods annotated with @Removed must be final.")
                        .hasErrorContaining(27, 10, "Methods annotated with @SystemProcessor must be final."))
                .executeTest();
    }

    @Test
    void testErrorOnInternalWorldParameter() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/InternalWorldParameterSystem.java")
                .compilationFails()
                .source("/system/common/InternalWorldParameterSystem.java", it -> it
                        .hasErrors(4).hasWarnings(0)
                        .hasErrorContaining(14, 53, "Cannot use InternalWorld.")
                        .hasErrorContaining(19, 58, "Cannot use InternalWorld.")
                        .hasErrorContaining(24, 52, "Cannot use InternalWorld.")
                        .hasErrorContaining(28, 37, "Cannot use InternalWorld."))
                .executeTest();
    }

    @Test
    void testErrorOnInternalEntityParameter() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/InternalEntityParameterSystem.java")
                .compilationFails()
                .source("/system/common/InternalEntityParameterSystem.java", it -> it
                        .hasErrors(3).hasWarnings(0)
                        .hasErrorContaining(14, 54, "Cannot use InternalEntity.")
                        .hasErrorContaining(19, 59, "Cannot use InternalEntity.")
                        .hasErrorContaining(24, 53, "Cannot use InternalEntity."))
                .executeTest();
    }

    @Test
    void testWarningOnNoEntityDataInEntityProcessors() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/common/NoEntityDataSystem.java")
                .compilationSucceeds()
                .source("/system/common/NoEntityDataSystem.java", it -> it
                        .hasErrors(0).hasWarnings(0).hasNotes(3)
                        .hasNoteContaining(13, 16, "@Inserted method does not use any entity data.")
                        .hasNoteContaining(18, 16, "@EntityProcessor method does not use any entity data.")
                        .hasNoteContaining(23, 16, "@Removed method does not use any entity data."))
                .executeTest();
    }

}
