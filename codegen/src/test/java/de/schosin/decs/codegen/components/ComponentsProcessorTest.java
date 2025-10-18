package de.schosin.decs.codegen.components;

import de.schosin.decs.codegen.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.visitor.filter.TypeFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ComponentsProcessorTest {

    @Test
    void testInterfaceComponent() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/components/interfacecomponent")
                .compilationSucceeds()
                .source("/components/interfacecomponent/Position.java", it -> it.hasNoWarnings())
                .generatedClass("foo.PositionImpl", type -> {
                    assertThat(type.getDeclaredFields()).hasSize(3); // index, x, y
                    assertThat(type.getDeclaredField("index")).isNotNull();

                    var x = type.getMethodsByName("x");
                    assertThat(x).hasSize(2);

                    var y = type.getMethodsByName("y");
                    assertThat(x).hasSize(2);
                })
                .executeTest();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/components/unnamedpackage/utility", "/components/unnamedpackage/system"})
    void testErrorOnComponentInUnnamedPackage(String folder) {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders(folder)
                .compilationFails()
                .source(folder + "/Data.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(4, 8, "Components must not be in the unnamed package."))
                .executeTest();
    }

    @Test
    void testErrorOnSameSimpleName() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/components/samesimplename/foo")
                .sourceFilesFromFolders("/components/samesimplename/bar")
                .compilationFails()
                .source("/components/samesimplename/foo/Data.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(6, 8, "Components must have a unique simple name. Collision with: bar.Data"))
                .source("/components/samesimplename/bar/Data.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(6, 8, "Components must have a unique simple name. Collision with: foo.Data"))
                .executeTest();
    }

    @Test
    void testNoteOnUnusedComponent() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFilesFromFolders("/components/unusedcomponent")
                .compilationSucceeds()
                .source("/setup/components/Position.java", it -> it
                        .hasNoErrors().hasNoWarnings().hasNoNotes())
                .source("/setup/components/Velocity.java", it -> it
                        .hasNoErrors().hasNoNotes()
                        .hasWarningContaining(6, 8, "Component is being added to entities, but not used by entity processors."))
                .source("/setup/components/Acceleration.java", it -> it
                        .hasNoErrors().hasNoWarnings().hasNotes(1)
                        .hasNoteContaining(6, 8, "Component is not used."))
                .executeTest();
    }

}
