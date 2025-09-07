package de.schosin.decs.codegen.utility;

import org.junit.jupiter.api.Test;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

public class CommonUtilityTest {

    @Test
    void testErrorOnNonAbstractMethod() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/common/NonAbstractMethodUtility.java")
                .compilationFails()
                .source("/utility/common/NonAbstractMethodUtility.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(11, 10, "Methods annotated with @Archetype must be abstract.")
                        .hasErrorContaining(19, 10, "Methods annotated with @Transmute must be abstract."))
                .executeTest();
    }

    @Test
    void testErrorOnAbstractEntityInitializer() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/common/AbstractEntityInitializerUtility.java")
                .compilationFails()
                .source("/utility/common/AbstractEntityInitializerUtility.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(14, 19, "Methods annotated with @EntityInitializer must be final.")
                        .hasErrorContaining(20, 19, "Methods annotated with @EntityInitializer must be final."))
                .executeTest();
    }

    @Test
    void testErrorOnNonFinalEntityInitializer() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/common/NonFinalEntityInitializerUtility.java")
                .compilationFails()
                .source("/utility/common/NonFinalEntityInitializerUtility.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(14, 10, "Methods annotated with @EntityInitializer must be final.")
                        .hasErrorContaining(21, 10, "Methods annotated with @EntityInitializer must be final."))
                .executeTest();
    }

    @Test
    void testErrorOnPrivateEntityInitializer() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/common/PrivateEntityInitializerUtility.java")
                .compilationFails()
                .source("/utility/common/PrivateEntityInitializerUtility.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(14, 24, "Methods annotated with @EntityInitializer must not be private.")
                        .hasErrorContaining(21, 24, "Methods annotated with @EntityInitializer must not be private."))
                .executeTest();
    }

}
