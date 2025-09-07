package de.schosin.decs.codegen.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

class ValueProcessorTest {

    @ParameterizedTest
    @ValueSource(strings = { "/value/reservedkeyword", "/value/metaannotation/reservedkeyword" })
    void testErrorOnReservedKeywords(String folder) {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders(folder)
                .compilationFails()
                .source(folder + "/ReservedKeywords.java", it -> it
                        .hasErrorContaining(7, 55, "@Value cannot use java language keyword: abstract")
                        .hasErrorContaining(11, 51, "@Value cannot use java language keyword: assert")
                        .hasErrorContaining(15, 53, "@Value cannot use java language keyword: boolean")
                        .hasErrorContaining(19, 49, "@Value cannot use java language keyword: break")
                        .hasErrorContaining(23, 47, "@Value cannot use java language keyword: byte")
                        .hasErrorContaining(27, 47, "@Value cannot use java language keyword: case")
                        .hasErrorContaining(31, 49, "@Value cannot use java language keyword: catch")
                        .hasErrorContaining(35, 47, "@Value cannot use java language keyword: char")
                        .hasErrorContaining(39, 49, "@Value cannot use java language keyword: class")
                        .hasErrorContaining(43, 55, "@Value cannot use java language keyword: continue")
                        .hasErrorContaining(47, 49, "@Value cannot use java language keyword: const")
                        .hasErrorContaining(51, 53, "@Value cannot use java language keyword: default")
                        .hasErrorContaining(55, 43, "@Value cannot use java language keyword: do")
                        .hasErrorContaining(59, 51, "@Value cannot use java language keyword: double")
                        .hasErrorContaining(63, 47, "@Value cannot use java language keyword: else")
                        .hasErrorContaining(67, 47, "@Value cannot use java language keyword: enum")
                        .hasErrorContaining(71, 53, "@Value cannot use java language keyword: extends")
                        .hasErrorContaining(75, 49, "@Value cannot use java language keyword: final")
                        .hasErrorContaining(79, 53, "@Value cannot use java language keyword: finally")
                        .hasErrorContaining(83, 49, "@Value cannot use java language keyword: float")
                        .hasErrorContaining(87, 45, "@Value cannot use java language keyword: for")
                        .hasErrorContaining(91, 47, "@Value cannot use java language keyword: goto")
                        .hasErrorContaining(95, 43, "@Value cannot use java language keyword: if")
                        .hasErrorContaining(99, 59, "@Value cannot use java language keyword: implements")
                        .hasErrorContaining(103, 51, "@Value cannot use java language keyword: import")
                        .hasErrorContaining(107, 59, "@Value cannot use java language keyword: instanceof")
                        .hasErrorContaining(111, 45, "@Value cannot use java language keyword: int")
                        .hasErrorContaining(115, 57, "@Value cannot use java language keyword: interface")
                        .hasErrorContaining(119, 47, "@Value cannot use java language keyword: long")
                        .hasErrorContaining(127, 51, "@Value cannot use java language keyword: native")
                        .hasErrorContaining(131, 45, "@Value cannot use java language keyword: new")
                        .hasErrorContaining(135, 53, "@Value cannot use java language keyword: package")
                        .hasErrorContaining(139, 53, "@Value cannot use java language keyword: private")
                        .hasErrorContaining(143, 57, "@Value cannot use java language keyword: protected")
                        .hasErrorContaining(147, 51, "@Value cannot use java language keyword: public")
                        .hasErrorContaining(151, 51, "@Value cannot use java language keyword: return")
                        .hasErrorContaining(155, 49, "@Value cannot use java language keyword: short")
                        .hasErrorContaining(159, 51, "@Value cannot use java language keyword: static")
                        .hasErrorContaining(163, 55, "@Value cannot use java language keyword: strictfp")
                        .hasErrorContaining(167, 49, "@Value cannot use java language keyword: super")
                        .hasErrorContaining(171, 51, "@Value cannot use java language keyword: switch")
                        .hasErrorContaining(175, 63, "@Value cannot use java language keyword: synchronized")
                        .hasErrorContaining(179, 47, "@Value cannot use java language keyword: this")
                        .hasErrorContaining(183, 49, "@Value cannot use java language keyword: throw")
                        .hasErrorContaining(187, 51, "@Value cannot use java language keyword: throws")
                        .hasErrorContaining(191, 57, "@Value cannot use java language keyword: transient")
                        .hasErrorContaining(195, 45, "@Value cannot use java language keyword: try")
                        .hasErrorContaining(203, 47, "@Value cannot use java language keyword: void")
                        .hasErrorContaining(207, 55, "@Value cannot use java language keyword: volatile")
                        .hasErrorContaining(211, 49, "@Value cannot use java language keyword: while"))
                .executeTest();
    }

    @Test
    void testErrorOnDifferentTypes() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/values")
                .sourceFilesFromFolders("/value/differenttypes")
                .compilationFails()
                .source("/value/differenttypes/TestSystem.java", it -> it
                        .hasErrorContaining(16, 58, "@Value with name 'test' used with different types: int / float")
                        .hasErrorContaining(25, 57, "@Value with name 'delta' used with different types: java.lang.String / java.lang.Class<?>"))
                .executeTest();
    }

    @Test
    void testMetaAnnotation() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/values")
                .sourceFilesFromFolders("/value/metaannotation")
                .compilationSucceeds()
                .generatedClass("de.schosin.decs.values.Values", type -> {
                    assertThat(type.isPublic()).isTrue();
                    assertThat(type.getMethods()).isEmpty();
                    assertThat(type.getFields())
                            .hasSize(1)
                            .first()
                            .extracting("reference.final", "reference.static", "reference.type.actualClass", "reference.simpleName")
                            .contains(false, false, int.class, "delta");
                })
                .executeTest();
    }

    @Test
    void testArrayValues() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/values")
                .sourceFilesFromFolders("/value/arrayvalues")
                .compilationSucceeds()
                .generatedClass("de.schosin.decs.values.Values", type -> {
                    assertThat(type.isPublic()).isTrue();
                    assertThat(type.getMethods()).isEmpty();
                    assertThat(type.getFields())
                            .hasSize(3)
                            .extracting(Object::toString)
                            .containsExactlyInAnyOrder(
                                    "public int[] intArray;",
                                    "public Integer[] integerArray;",
                                    "public List<Integer>[] integerListArray;");
                })
                .executeTest();
    }

}
