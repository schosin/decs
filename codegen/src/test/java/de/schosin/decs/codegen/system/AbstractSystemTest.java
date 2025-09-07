package de.schosin.decs.codegen.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.schosin.decs.codegen.CuteTest;
import de.schosin.decs.codegen.DecsAnnotationProcessor;

abstract class AbstractSystemTest {

    protected final String base;
    protected final String annotation;

    protected AbstractSystemTest(String base, String annotation) {
        this.base = base;
        this.annotation = annotation;
    }

    @ParameterizedTest
    @CsvSource({
            "FinalClassTypeSystem,6,14"
    })
    void testErrorOnInvalidFinalType(String type, int line, int column) {
        var source = this.base + "/invalidtype/%s.java".formatted(type);

        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(source)
                .compilationFails()
                .source(source, it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(line, column, "Types using @%s must not be final.".formatted(this.annotation)))
                .executeTest();
    }

    @ParameterizedTest
    @CsvSource({
            "EnumTypeSystem,6,8", "RecordTypeSystem,6,8", "InterfaceTypeSystem,6,8", "AnnotationTypeSystem,6,9"
    })
    void testErrorOnInvalidNonFinalType(String type, int line, int column) {
        var source = this.base + "/invalidtype/%s.java".formatted(type);

        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(source)
                .compilationFails()
                .source(source, it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(line, column, "Types using @%s must be an abstract class.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnNonPublicSystemType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/NonPublicSystem.java")
                .compilationFails()
                .source(this.base + "/errors/NonPublicSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(6, 1, "Types using @%s must be public.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnNonPublicNestedSystemType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/NonPublicNestedSystem.java")
                .compilationFails()
                .source(this.base + "/errors/NonPublicNestedSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 12, "Types using @%s must be public.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnNonStaticNestedSystemType() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/NonStaticNestedSystem.java")
                .compilationFails()
                .source(this.base + "/errors/NonStaticNestedSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 12, "Types using @%s must be accessible. Nested types must be static.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnNestedSystemTypeInNonPublicParent() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/NonPublicParentSystem.java")
                .compilationFails()
                .source(this.base + "/errors/NonPublicParentSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 19, "Types using @%s must be accessible. Type is nested in a non-public parent type.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnOfferArchetypeMethod() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/OfferArchetypeMethodSystem.java")
                .compilationFails()
                .source(this.base + "/errors/OfferArchetypeMethodSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(14, 24, "Types using @%s must not have a method called \"offerArchetype\" with a EntityArchetype argument.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnRunSystemMethod() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/RunSystemMethodSystem.java")
                .compilationFails()
                .source(this.base + "/errors/RunSystemMethodSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(13, 23, "Types using @%s must not have a method called \"runSystem\" with no arguments.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnImplementsCallable() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/CallableSystem.java")
                .compilationFails()
                .source(this.base + "/errors/CallableSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(8, 8, "Types using @%s must not implement Callable.".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnCallMethod() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFiles(this.base + "/errors/CallMethodSystem.java")
                .compilationFails()
                .source(this.base + "/errors/CallMethodSystem.java", it -> it
                        .hasErrors(1).hasWarnings(0)
                        .hasErrorContaining(13, 17, "Types using @%s must not have a method called \"call\" with no arguments (Callable signature).".formatted(this.annotation)))
                .executeTest();
    }

    @Test
    void testErrorOnNonAbstractUtility() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles(this.base + "/errors/NonAbstractUtilitySystem.java")
                .compilationFails()
                .source(this.base + "/errors/NonAbstractUtilitySystem.java", it -> it
                        .hasErrors(2).hasWarnings(0) // ignore error on @Transmute, only non-abstract class
                        .hasErrorContaining(10, 8, "Types using @Archetype must be an abstract class."))
                .executeTest();
    }

    @Test
    void testErrorOnNonAbstractUtilityMethods() {
        CuteTest.blackBoxTest()
                .processor(DecsAnnotationProcessor.class)
                .sourceFilesFromFolders("/setup/components")
                .sourceFiles(this.base + "/errors/NonAbstractUtilityMethodsSystem.java")
                .compilationFails()
                .source(this.base + "/errors/NonAbstractUtilityMethodsSystem.java", it -> it
                        .hasErrors(2).hasWarnings(0)
                        .hasErrorContaining(18, 10, "Methods annotated with @Archetype must be abstract.")
                        .hasErrorContaining(26, 10, "Methods annotated with @Transmute must be abstract."))
                .executeTest();
    }

}
