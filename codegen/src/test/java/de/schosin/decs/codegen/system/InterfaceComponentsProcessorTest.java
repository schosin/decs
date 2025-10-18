package de.schosin.decs.codegen.system;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

public class InterfaceComponentsProcessorTest {

    @Test
    void testInterfaceComponentProcessing() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/system/entityprocessor/interfacecomponents")
                .compilationSucceeds()
                .source("/system/entityprocessor/interfacecomponents/System.java", it -> it.hasNoWarnings())
                .source("/system/entityprocessor/interfacecomponents/Position.java", it -> it.hasNoWarnings())
                .source("/system/entityprocessor/interfacecomponents/Velocity.java", it -> it.hasNoWarnings())
                .generatedClass("foo.SystemImpl", type -> {
                    // fail("not implemented yet" + System.lineSeparator() + System.lineSeparator() + type);
                })
                .executeTest();
    }

}
