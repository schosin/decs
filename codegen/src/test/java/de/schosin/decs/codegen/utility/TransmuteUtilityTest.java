package de.schosin.decs.codegen.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

import spoon.reflect.declaration.CtType;

public class TransmuteUtilityTest {

    @Nested
    class CommonTransmuteTest extends AbstractUtilityTest {

        protected CommonTransmuteTest() {
            super("/utility/transmute", "Transmute");
        }

    }

    @Test
    void testTransmuteUtility() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/SimpleTransmute.java")
                .compilationSucceeds()
                .source("/utility/transmute/SimpleTransmute.java", it -> it.hasNoWarnings())
                .generatedClass("foo.SimpleTransmuteImpl", type -> {
                    assertThat(type.getMethods()).hasSize(4);
                    assertThat(type.getMethodsByName("transmute1")).hasSize(1);
                    assertThat(type.getMethodsByName("transmute2")).hasSize(1);
                    assertThat(type.getMethodsByName("transmute3")).hasSize(1);

                    assertThat(type.getNestedTypes()).hasSize(3);
                    assertThat(type.<CtType<?>>getNestedType("Transmute1Transmuter")).isNotNull();
                    assertThat(type.<CtType<?>>getNestedType("Transmute2Transmuter")).isNotNull();
                    assertThat(type.<CtType<?>>getNestedType("Transmute3Transmuter")).isNotNull();
                })
                .executeTest();
    }

    @Test
    void testOverloadedMethods() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/OverloadedMethodTransmute.java")
                .compilationSucceeds()
                .source("/utility/transmute/OverloadedMethodTransmute.java", it -> it.hasNoWarnings())
                .generatedClass("foo.OverloadedMethodTransmuteImpl", type -> {
                    assertThat(type.getMethods()).hasSize(4);
                    assertThat(type.getMethodsByName("transmute")).hasSize(3);

                    assertThat(type.getNestedTypes()).hasSize(3);
                    assertThat(type.<CtType<?>>getNestedType("TransmuteTransmuter")).isNotNull();
                    assertThat(type.<CtType<?>>getNestedType("TransmuteTransmuter2")).isNotNull();
                    assertThat(type.<CtType<?>>getNestedType("TransmuteTransmuter3")).isNotNull();
                })
                .executeTest();
    }

    @Test
    void testErrorOnNonVoidReturnType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/errors/NonVoidReturnTransmute.java")
                .compilationFails()
                .source("/utility/transmute/errors/NonVoidReturnTransmute.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(10, 18, "Methods annotated with @Transmute must return void."))
                .executeTest();
    }

    @Test
    void testErrorOnFirstParaterNotEntity() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/errors/FirstNotEntityTransmute.java")
                .compilationFails()
                .source("/utility/transmute/errors/FirstNotEntityTransmute.java", it -> it
                        .hasErrors(9).hasWarnings(0)
                        .hasErrorContaining(10, 19, "@Transmute methods must start with \"Entity entity\", \"EntityRef entity\", or \"BaseEntity entity\".")
                        .hasErrorContaining(17, 19, "@Transmute methods must start with \"Entity entity\", \"EntityRef entity\", or \"BaseEntity entity\".")
                        .hasErrorContaining(24, 19, "@Transmute methods must start with \"Entity entity\", \"EntityRef entity\", or \"BaseEntity entity\".")
                        .hasErrorContaining(34, 16, "@EntityInitializer methods for @Transmute must start with \"int entityId\".")
                        .hasErrorContaining(41, 16, "@EntityInitializer methods for @Transmute must start with \"int entityId\".")
                        .hasErrorContaining(48, 16, "@EntityInitializer methods for @Transmute must start with \"int entityId\".")
                        .hasErrorContaining(52, 19, "@Transmute methods must start with \"Entity entity\", \"EntityRef entity\", or \"BaseEntity entity\".")
                        .hasErrorContaining(62, 16, "@EntityInitializer methods for @Transmute must start with \"int entityId\".")
                        .hasErrorContaining(69, 16, "@EntityInitializer methods for @Transmute must start with \"int entityId\"."))
                .executeTest();
    }

    @Test
    void testErrorOnDuplicateComponent() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/errors/DuplicateComponentTransmute.java")
                .compilationFails()
                .source("/utility/transmute/errors/DuplicateComponentTransmute.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(14, 83, "The same component type cannot be used multiple times: foo.Position")
                        .hasErrorContaining(21, 83, "The same component type cannot be used multiple times: foo.Velocity"))
                .executeTest();
    }

    @Test
    void testErrorOnNoChanges() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/errors/NoChangesTransmute.java")
                .compilationFails()
                .source("/utility/transmute/errors/NoChangesTransmute.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(10, 19, "@Transmute must atleast add (@EntityInitializer parameter) or remove (@Transmute(remove = {...}) a component."))
                .executeTest();
    }

    @Test
    void testErrorOnNonComponentParameters() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components", "/setup/values", "/setup/systems", "/setup/utilities")
                .sourceFiles("/utility/transmute/errors/NonComponentParameterTransmute.java")
                .compilationFails()
                .source("/utility/transmute/errors/NonComponentParameterTransmute.java", it -> it
                        .hasErrors(9).hasNoWarnings()
                        .hasErrorContaining(16, 57, "Type \"foo.ExampleArchetype\" is not a component. Remove the parameter, or annotate it with @Component.")
                        .hasErrorContaining(23, 57, "Type \"foo.ExampleTransmute\" is not a component. Remove the parameter, or annotate it with @Component.")
                        .hasErrorContaining(30, 47, "Primitive \"int\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(37, 49, "Primitive \"long\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(44, 44, "Non-source type \"java.lang.String\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(51, 42, "Non-source type \"de.schosin.decs.api.World\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(58, 58, "Non-source type \"de.schosin.decs.api.internal.InternalWorld\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(65, 44, "Non-source type \"de.schosin.decs.api.entities.Entity\" cannot be used as a component. Remove the parameter.")
                        .hasErrorContaining(72, 60, "Non-source type \"de.schosin.decs.api.internal.InternalEntity\" cannot be used as a component. Remove the parameter."))
                .executeTest();
    }

    @Test
    void testErrorOnSameComponentAddedAndRemoved() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/utility/transmute/errors/AddRemoveSameComponentSystem.java")
                .compilationFails()
                .source("/utility/transmute/errors/AddRemoveSameComponentSystem.java", it -> it
                        .hasErrors(5).hasWarnings(0)
                        .hasErrorContaining(13, 50, "Component type 'foo.Position' cannot be used as a parameter (add) and in @Transmute (remove).")
                        .hasErrorContaining(20, 50, "Component type 'foo.Position' cannot be used as a parameter (add) and in @Transmute (remove).")
                        .hasErrorContaining(27, 64, "Component type 'foo.Velocity' cannot be used as a parameter (add) and in @Transmute (remove).")
                        .hasErrorContaining(34, 50, "Component type 'foo.Position' cannot be used as a parameter (add) and in @Transmute (remove).")
                        .hasErrorContaining(34, 64, "Component type 'foo.Velocity' cannot be used as a parameter (add) and in @Transmute (remove)."))
                .executeTest();
    }

}
