package de.schosin.decs.codegen.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.decs.api.entities.EntityArchetypeListener;
import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;
import de.schosin.decs.codegen.utils.Utils;

import spoon.reflect.code.CtBlock;
import spoon.reflect.visitor.filter.TypeFilter;

class RemovedTest {

    @Nested
    class CommonInsertedTest extends AbstractProcessorTest {

        protected CommonInsertedTest() {
            super("/system/removed", "Removed");
        }

    }

    @Test
    void testMinimalSystemType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles("/system/removed/MinimalSystem.java")
                .compilationSucceeds()
                .source("/system/removed/MinimalSystem.java", it -> it.hasNoWarnings())
                .generatedClass("foo.MinimalSystemImpl", type -> {
                    assertThat(type.getSuperInterfaces()).extracting("qualifiedName").containsExactly(Utils.SYSTEM_TYPE.canonicalName());
                    assertThat(type.getMethods()).hasSize(2);

                    assertThat(type.getMethodsByName("offerArchetype")).hasSize(1);

                    var runSystem = type.getMethodsByName("runSystem").getFirst();
                    assertThat(runSystem).isNotNull();

                    var runSystemImpl = assertThat(runSystem.<CtBlock<?>>getElements(new TypeFilter<>(CtBlock.class))).hasSize(1).first().actual();
                    assertThat(runSystemImpl.getElements(element -> !CtBlock.class.isAssignableFrom(element.getClass()))).isEmpty();

                    var nestedTypes = type.getNestedTypes();
                    assertThat(nestedTypes).hasSize(1);

                    var nestedType = nestedTypes.iterator().next();
                    assertThat(nestedType.getSuperInterfaces())
                            .hasSize(1)
                            .allSatisfy(iface -> assertThat(iface.getActualClass()).isEqualTo(EntityArchetypeListener.class));
                })
                .executeTest();
    }

}
