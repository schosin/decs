package de.schosin.decs.codegen.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;
import de.schosin.decs.codegen.utils.Utils;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.visitor.filter.TypeFilter;

class SystemProcessorTest {

    @Nested
    class CommonSystemProcessorTest extends AbstractSystemTest {

        protected CommonSystemProcessorTest() {
            super("/system/systemprocessor", "SystemProcessor");
        }

    }

    @Test
    void testSimpleSystemProcessor() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/systemprocessor/MinimalSystem.java")
                .compilationSucceeds()
                .source("/system/systemprocessor/MinimalSystem.java", it -> it.hasNoWarnings())
                .generatedClass("foo.MinimalSystemImpl", type -> {
                    assertThat(type.getSuperInterfaces()).extracting("qualifiedName").containsExactly(Utils.SYSTEM_TYPE.canonicalName());
                    assertThat(type.getMethods()).hasSize(2);

                    assertThat(type.getMethodsByName("offerArchetype")).hasSize(1);

                    var method = type.getMethodsByName("runSystem").getFirst();
                    assertThat(method).isNotNull();

                    var invocations = method.getElements(new TypeFilter<>(CtInvocation.class));
                    assertThat(invocations).hasSize(1);

                    var invocation = invocations.getFirst();
                    assertThat(invocation.getExecutable().getSimpleName()).isEqualTo("system");
                })
                .executeTest();
    }

    @Test
    void testErrorOnComponentParameter() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/system/systemprocessor/errors/ComponentParameterSystem.java")
                .compilationFails()
                .source("/system/systemprocessor/errors/ComponentParameterSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 32, "Cannot use a component type as a system parameter."))
                .executeTest();
    }

    @Test
    void testErrorOnEntityIdParameter() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/system/systemprocessor/errors/EntityIdParameterSystem.java")
                .compilationFails()
                .source("/system/systemprocessor/errors/EntityIdParameterSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 27, "Cannot use non-@Value int as a system parameter."))
                .executeTest();
    }

    @Test
    void testErrorOnEntityParameter() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/system/systemprocessor/errors/EntityParameterSystem.java")
                .compilationFails()
                .source("/system/systemprocessor/errors/EntityParameterSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(9, 30, "Cannot use Entity as a system parameter."))
                .executeTest();
    }

    @Test
    void testErrorOnPrimitiveLongParameter() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles("/system/systemprocessor/errors/PrimitiveLongParameterSystem.java")
                .compilationFails()
                .source("/system/systemprocessor/errors/PrimitiveLongParameterSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 28, "Invalid primitive parameter 'value': Primitive values are only supported with @Value"))
                .executeTest();
    }

}
