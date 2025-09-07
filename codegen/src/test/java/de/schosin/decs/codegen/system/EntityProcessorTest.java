package de.schosin.decs.codegen.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;
import de.schosin.decs.codegen.utils.Utils;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.visitor.filter.TypeFilter;

class EntityProcessorTest {

    @Nested
    class CommonEntityProcessorTest extends AbstractProcessorTest {

        protected CommonEntityProcessorTest() {
            super("/system/entityprocessor", "EntityProcessor");
        }

    }

    @Test
    void testMinimalSystemType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/entityprocessor/MinimalSystem.java")
                .compilationSucceeds()
                .source("/system/entityprocessor/MinimalSystem.java", it -> it.hasNoWarnings())
                .generatedClass("foo.MinimalSystemImpl", type -> {
                    assertThat(type.getSuperInterfaces()).extracting("qualifiedName").containsExactly(Utils.SYSTEM_TYPE.canonicalName());
                    assertThat(type.getMethods()).hasSize(2);

                    assertThat(type.getMethodsByName("offerArchetype")).hasSize(1);

                    var runSystem = type.getMethodsByName("runSystem").getFirst();
                    assertThat(runSystem).isNotNull();

                    var invocations = runSystem.getElements(new TypeFilter<>(CtInvocation.class));
                    assertThat(invocations).filteredOn(invocation -> "run".equals(invocation.getExecutable().getSimpleName())).hasSize(1);

                    var nestedTypes = type.getNestedTypes();
                    assertThat(nestedTypes).hasSize(1);

                    var nestedType = nestedTypes.iterator().next();

                    var run = nestedType.getMethodsByName("run").getFirst();
                    var runInvocations = run.getElements(new TypeFilter<>(CtInvocation.class));
                    assertThat(runInvocations).filteredOn(invocation -> "processEntity".equals(invocation.getExecutable().getSimpleName())).hasSize(1);
                })
                .executeTest();
    }

}
