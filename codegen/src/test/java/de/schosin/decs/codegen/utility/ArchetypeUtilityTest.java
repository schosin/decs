package de.schosin.decs.codegen.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

import spoon.reflect.declaration.CtType;

public class ArchetypeUtilityTest {

    @Nested
    class CommonArchetypeTest extends AbstractUtilityTest {

        protected CommonArchetypeTest() {
            super("/utility/archetype", "Archetype");
        }

    }

    @Test
    void testArchetypeUtility() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/archetype/SimpleArchetype.java")
                .compilationSucceeds()
                .source("/utility/archetype/SimpleArchetype.java", it -> it.hasNoWarnings())
                .generatedClass("foo.SimpleArchetypeImpl", type -> {
                    assertThat(type.getMethods()).hasSize(2);
                    assertThat(type.getMethodsByName("create")).hasSize(1);
                })
                .executeTest();
    }

    @Test
    void testOverloadedMethods() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/archetype/OverloadedMethodArchetype.java")
                .compilationSucceeds()
                .source("/utility/archetype/OverloadedMethodArchetype.java", it -> it.hasNoWarnings())
                .generatedClass("foo.OverloadedMethodArchetypeImpl", type -> {
                    assertThat(type.getMethods()).hasSize(4);
                    assertThat(type.getMethodsByName("create")).hasSize(3);

                    assertThat(type.getNestedTypes()).hasSize(3);
                    assertThat(type.<CtType<?>>getNestedType("CreateArchetype")).isNotNull();
                    assertThat(type.<CtType<?>>getNestedType("CreateArchetype2")).isNotNull();
                    assertThat(type.<CtType<?>>getNestedType("CreateArchetype3")).isNotNull();
                })
                .executeTest();
    }

    @Test
    void testErrorOnInvalidReturnType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/archetype/errors/InvalidReturnTypeArchetype.java")
                .compilationFails()
                .source("/utility/archetype/errors/InvalidReturnTypeArchetype.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(9, 19, "Methods annotated with @Archetype must return void or EntityBag."))
                .executeTest();
    }

    @Test
    void testErrorOnFirstNotCount() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/archetype/errors/FirstNotCountArchetype.java")
                .compilationFails()
                .source("/utility/archetype/errors/FirstNotCountArchetype.java", it -> it
                        .hasErrors(7).hasWarnings(0)
                        .hasErrorContaining(9, 26, "@Archetype methods must start with \"int count\".")
                        .hasErrorContaining(16, 26, "@Archetype methods must start with \"int count\".")
                        .hasErrorContaining(23, 26, "@Archetype methods must start with \"int count\".")
                        .hasErrorContaining(30, 26, "@Archetype methods must start with \"int count\".")
                        .hasErrorContaining(37, 26, "@Archetype methods must start with \"int count\".")
                        .hasErrorContaining(47, 26, "@EntityInitializer methods for @Archetype must start with \"int index\".")
                        .hasErrorContaining(54, 26, "@EntityInitializer methods for @Archetype must start with \"int index\"."))
                .executeTest();
    }

    @Test
    void testErrorOnDuplicateComponent() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/archetype/errors/DuplicateComponentArchetype.java")
                .compilationFails()
                .source("/utility/archetype/errors/DuplicateComponentArchetype.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(12, 77, "The same component type cannot be used multiple times: foo.Position")
                        .hasErrorContaining(19, 77, "The same component type cannot be used multiple times: foo.Velocity"))
                .executeTest();
    }

    @Test
    void testErrorOnNoComponents() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/archetype/errors/NoComponentsArchetype.java")
                .compilationFails()
                .source("/utility/archetype/errors/NoComponentsArchetype.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(12, 26, "@Archetype must add atleast one component (@EntityInitializer parameter)."))
                .executeTest();
    }

    @Test
    void testErrorOnNonComponentParameters() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components", "/setup/values", "/setup/systems", "/setup/utilities")
                .sourceFiles("/utility/archetype/errors/NonComponentParameterArchetype.java")
                .compilationFails()
                .source("/utility/archetype/errors/NonComponentParameterArchetype.java", it -> it
                        .hasErrors(9).hasNoWarnings()
                        .hasErrorContaining(16, 54, "Type \"foo.ExampleArchetype\" is not a component. Remove the parameter, or annotate it with @Component.")
                        .hasErrorContaining(23, 54, "Type \"foo.ExampleTransmute\" is not a component. Remove the parameter, or annotate it with @Component.")
                        .hasErrorContaining(30, 44, "Primitive \"int\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(37, 46, "Primitive \"long\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(44, 41, "Non-source type \"java.lang.String\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(51, 39, "Non-source type \"de.schosin.decs.api.World\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(58, 55, "Non-source type \"de.schosin.decs.api.internal.InternalWorld\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(65, 41, "Non-source type \"de.schosin.decs.api.entities.Entity\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(72, 57, "Non-source type \"de.schosin.decs.api.internal.InternalEntity\" cannot be used as a component. Remove the parameter."))
                .executeTest();
    }

}
