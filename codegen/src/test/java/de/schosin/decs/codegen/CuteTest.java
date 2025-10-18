package de.schosin.decs.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import io.toolisticon.cute.Cute;
import io.toolisticon.cute.CuteApi.BlackBoxTestFinalGivenInterface;
import io.toolisticon.cute.CuteApi.BlackBoxTestProcessorsInterface;
import io.toolisticon.cute.CuteApi.BlackBoxTestRootInterface;
import io.toolisticon.cute.CuteApi.BlackBoxTestSourceFilesInterface;
import io.toolisticon.cute.CuteApi.CompilerMessageCheckComparisonType;
import io.toolisticon.cute.CuteApi.CompilerTestExpectAndThatInterface;
import io.toolisticon.cute.CuteApi.CompilerTestExpectThatInterface;
import io.toolisticon.cute.CuteApi.CustomAssertion;
import io.toolisticon.cute.CuteApi.DoCustomAssertions;
import io.toolisticon.cute.CuteApi.ExceptionAssertion;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

/**
 * Helper class around {@link Cute#blackBoxTest()} to adjust the API and provide additional assertion methods.
 */
public final class CuteTest {

    private final BlackBoxTestRootInterface test;

    public static CuteTest blackBoxTest() {
        return new CuteTest();
    }

    private CuteTest() {
        this.test = Cute.blackBoxTest();
    }

    public BlackBoxSourceFiles processors(Iterable<Class<? extends Processor>> processors) {
        return new BlackBoxSourceFiles(test.given().processors(processors));
    }

    public BlackBoxSourceFiles processor(Class<? extends Processor> processor) {
        return new BlackBoxSourceFiles(test.given().processor(DecsAnnotationProcessor.class));
    }

    public BlackBoxSourceFiles noProcessors() {
        return new BlackBoxSourceFiles(test.given().noProcessors());
    }

    public static final class BlackBoxProcessors {

        private final BlackBoxTestProcessorsInterface given;

        private BlackBoxProcessors(BlackBoxTestRootInterface test) {
            this.given = test.given();
        }

        @SuppressWarnings("unchecked")
        public BlackBoxSourceFiles processors(Class<? extends Processor>... processors) {
            return new BlackBoxSourceFiles(this.given.processors(List.of(processors)));
        }

        public BlackBoxSourceFiles processors(Iterable<Class<? extends Processor>> processors) {
            return new BlackBoxSourceFiles(this.given.processors(processors));
        }

        public BlackBoxSourceFiles processor(Class<? extends Processor> processor) {
            return new BlackBoxSourceFiles(this.given.processor(DecsAnnotationProcessor.class));
        }

        public BlackBoxSourceFiles noProcessors() {
            return new BlackBoxSourceFiles(this.given.noProcessors());
        }

    }

    public static final class BlackBoxSourceFiles {

        private final BlackBoxTestSourceFilesInterface sourceFiles;

        private BlackBoxSourceFiles(BlackBoxTestSourceFilesInterface processors) {
            this.sourceFiles = processors;
        }

        public BlackBoxFinalGiven sourceFiles(JavaFileObject... sourceFile) {
            return new BlackBoxFinalGiven(this.sourceFiles.andSourceFiles(sourceFile));
        }

        public BlackBoxFinalGiven sourceFiles(String... resources) {
            return new BlackBoxFinalGiven(this.sourceFiles.andSourceFiles(resources));
        }

        public BlackBoxFinalGiven sourceFile(String className, String content) {
            return new BlackBoxFinalGiven(this.sourceFiles.andSourceFile(className, content));
        }

        public BlackBoxFinalGiven sourceFilesFromFolders(String... folders) {
            return new BlackBoxFinalGiven(this.sourceFiles.andSourceFilesFromFolders(folders));
        }

        public BlackBoxFinalGiven resourceFile(String targetPackageNameOrAbsolutePath, String resource) {
            return new BlackBoxFinalGiven(this.sourceFiles.andResourceFile(targetPackageNameOrAbsolutePath, resource));
        }

    }

    public static final class BlackBoxFinalGiven {

        private final BlackBoxTestFinalGivenInterface finalGiven;
        private final List<CustomAssertion> assertions = new ArrayList<>();

        private BlackBoxFinalGiven(BlackBoxTestFinalGivenInterface finalGiven) {
            this.finalGiven = finalGiven;
        }

        public BlackBoxFinalGiven sourceFiles(JavaFileObject... sourceFile) {
            return new BlackBoxFinalGiven(this.finalGiven.andSourceFiles(sourceFile));
        }

        public BlackBoxFinalGiven sourceFiles(String... resources) {
            return new BlackBoxFinalGiven(this.finalGiven.andSourceFiles(resources));
        }

        public BlackBoxFinalGiven sourceFile(String className, String content) {
            return new BlackBoxFinalGiven(this.finalGiven.andSourceFile(className, content));
        }

        public BlackBoxFinalGiven sourceFilesFromFolders(String... folders) {
            return new BlackBoxFinalGiven(this.finalGiven.andSourceFilesFromFolders(folders));
        }

        public BlackBoxFinalGiven resourceFile(String targetPackageNameOrAbsolutePath, String resource) {
            return new BlackBoxFinalGiven(this.finalGiven.andResourceFile(targetPackageNameOrAbsolutePath, resource));
        }

        public BlackBoxFinalGiven useCompilerOptions(String... compilerOptions) {
            return new BlackBoxFinalGiven(this.finalGiven.andUseCompilerOptions(compilerOptions));
        }

        public BlackBoxFinalGiven useModules(String... modules) {
            return new BlackBoxFinalGiven(this.finalGiven.andUseModules(modules));
        }

        public BlackBoxTestExpectAndThat compilationSucceeds() {
            return new BlackBoxTestExpectAndThat(this.finalGiven.whenCompiled().thenExpectThat().compilationSucceeds(), new ArrayList<>());
        }

        public BlackBoxTestExpectAndThat compilationFails() {
            return new BlackBoxTestExpectAndThat(this.finalGiven.whenCompiled().thenExpectThat().compilationFails(), new ArrayList<>());
        }

        public BlackBoxTestExpectAndThat exceptionIsThrown(Class<? extends Exception> exception) {
            return new BlackBoxTestExpectAndThat(this.finalGiven.whenCompiled().thenExpectThat().exceptionIsThrown(exception), new ArrayList<>());
        }

        public <T extends Exception> BlackBoxTestExpectAndThat exceptionIsThrown(Class<T> exception, ExceptionAssertion<T> exceptionAssertion) {
            return new BlackBoxTestExpectAndThat(this.finalGiven.whenCompiled().thenExpectThat().exceptionIsThrown(exception, exceptionAssertion), new ArrayList<>());
        }

        public BlackBoxCustomAssertions executeTest() {
            return new BlackBoxCustomAssertions(this.finalGiven.executeTest(), assertions);
        }

    }

    public static final class BlackBoxTestExpectAndThat {

        private final CompilerTestExpectAndThatInterface expectThat;
        private final List<CustomAssertion> assertions;

        private BlackBoxTestExpectAndThat(CompilerTestExpectAndThatInterface expectThat, List<CustomAssertion> assertions) {
            this.expectThat = expectThat;
            this.assertions = assertions;
        }

        public BlackBoxTestExpectAndThat source(String source, UnaryOperator<BlackBoxTestFileAssertionsAnd> op) {
            return new BlackBoxTestExpectAndThat(op.apply(new BlackBoxTestFileAssertionsAnd(expectThat, source, assertions)).expectThat, this.assertions);
        }

        public BlackBoxTestExpectAndThat generatedCode(String className, Consumer<String> code) {
            var andThat = expectThat.andThat().generatedSourceFile(className).matches(file -> {
                code.accept(file.getCharContent(false).toString());
                return true;
            });

            return new BlackBoxTestExpectAndThat(andThat, this.assertions);
        }

        public BlackBoxTestExpectAndThat generatedClass(String className, Consumer<CtClass<?>> type) {
            var andThat = expectThat.andThat().generatedSourceFile(className).matches(file -> {
                type.accept(Launcher.parseClass(file.getCharContent(false).toString()));
                return true;
            });

            return new BlackBoxTestExpectAndThat(andThat, this.assertions);
        }

        public BlackBoxCustomAssertions executeTest() {
            return new BlackBoxCustomAssertions(expectThat.executeTest(), assertions);
        }

    }

    public static final class BlackBoxTestFileAssertions {

        private final CompilerTestExpectThatInterface expectThat;
        private final String source;
        private final List<CustomAssertion> assertions;

        private BlackBoxTestFileAssertions(CompilerTestExpectThatInterface expectThat, String source, List<CustomAssertion> assertions) {
            this.expectThat = expectThat;
            this.source = source;
            this.assertions = assertions;

            this.assertions.add(it -> assertThat(it.getFileManager().getGeneratedSourceFile(source)).as("File '%s' was generated", source).isPresent());
        }

        public BlackBoxTestFileAssertionsAnd hasWarningContaining(String... text) {
            return hasMessageContaining(expectThat.compilerMessage().ofKindWarning(), text);
        }

        public BlackBoxTestFileAssertionsAnd hasErrorContaining(String... text) {
            return hasMessageContaining(expectThat.compilerMessage().ofKindError(), text);
        }

        public BlackBoxTestFileAssertionsAnd hasNoteContaining(String... text) {
            return hasMessageContaining(expectThat.compilerMessage().ofKindNote(), text);
        }

        private BlackBoxTestFileAssertionsAnd hasMessageContaining(CompilerMessageCheckComparisonType check, String... text) {
            return new BlackBoxTestFileAssertionsAnd(check.contains(text), source, List.of());
        }

        public BlackBoxTestFileAssertionsAnd hasWarningContaining(int line, int column, String... text) {
            return hasMessageContaining(expectThat.compilerMessage().ofKindWarning(), line, column, text);
        }

        public BlackBoxTestFileAssertionsAnd hasErrorContaining(int line, int column, String... text) {
            return hasMessageContaining(expectThat.compilerMessage().ofKindError(), line, column, text);
        }

        public BlackBoxTestFileAssertionsAnd hasNoteContaining(int line, int column, String... text) {
            return hasMessageContaining(expectThat.compilerMessage().ofKindNote(), line, column, text);
        }

        private BlackBoxTestFileAssertionsAnd hasMessageContaining(CompilerMessageCheckComparisonType check, int line, int column, String... text) {
            return new BlackBoxTestFileAssertionsAnd(check
                    .atSource(source)
                    .atLine(line)
                    .atColumn(column)
                    .contains(text), source, List.of());
        }

        public BlackBoxTestFileAssertions hasWarnings(int expected) {
            this.assertions.add(it -> {
                var warnings = it.getCompilerMessages().stream()
                        .filter(message -> (message.getKind() == Kind.WARNING || message.getKind() == Kind.MANDATORY_WARNING) && this.source.equals(message.getSource()))
                        .count();

                assertThat(warnings).as("%s has %d warnings", source, expected).isEqualTo(expected);
            });

            return this;
        }

        public BlackBoxTestFileAssertions hasErrors(long expected) {
            this.assertions.add(it -> {
                var errors = it.getCompilerMessages().stream()
                        .filter(message -> message.getKind() == Kind.ERROR && this.source.equals(message.getSource()))
                        .count();

                assertThat(errors).as("%s has %d errors", source, expected).isEqualTo(expected);
            });

            return this;
        }

        public BlackBoxTestFileAssertions hasNotes(long expected) {
            this.assertions.add(it -> {
                var errors = it.getCompilerMessages().stream()
                        .filter(message -> message.getKind() == Kind.NOTE && this.source.equals(message.getSource()))
                        .count();

                assertThat(errors).as("%s has %d notes", source, expected).isEqualTo(expected);
            });

            return this;
        }

        public BlackBoxTestFileAssertions hasNoWarnings() {
            return hasWarnings(0);
        }

        public BlackBoxTestFileAssertions hasNoErrors() {
            return hasErrors(0);
        }

        public BlackBoxTestFileAssertions hasNoNotes() {
            return hasNotes(0);
        }

    }

    public static final class BlackBoxTestFileAssertionsAnd {

        private final CompilerTestExpectAndThatInterface expectThat;
        private final String source;
        private final List<CustomAssertion> assertions;

        private BlackBoxTestFileAssertionsAnd(CompilerTestExpectAndThatInterface expectThat, String source, List<CustomAssertion> assertions) {
            this.expectThat = expectThat;
            this.source = source;
            this.assertions = assertions;
        }

        public BlackBoxTestFileAssertionsAnd hasWarningContaining(String... text) {
            return hasMessageContaining(expectThat.andThat().compilerMessage().ofKindWarning(), text);
        }

        public BlackBoxTestFileAssertionsAnd hasErrorContaining(String... text) {
            return hasMessageContaining(expectThat.andThat().compilerMessage().ofKindError(), text);
        }

        public BlackBoxTestFileAssertionsAnd hasNoteContaining(String... text) {
            return hasMessageContaining(expectThat.andThat().compilerMessage().ofKindNote(), text);
        }

        private BlackBoxTestFileAssertionsAnd hasMessageContaining(CompilerMessageCheckComparisonType check, String... text) {
            return new BlackBoxTestFileAssertionsAnd(check.contains(text), source, this.assertions);
        }

        public BlackBoxTestFileAssertionsAnd hasWarningContaining(int line, int column, String... text) {
            return hasMessageContaining(expectThat.andThat().compilerMessage().ofKindWarning(), line, column, text);
        }

        public BlackBoxTestFileAssertionsAnd hasErrorContaining(int line, int column, String... text) {
            return hasMessageContaining(expectThat.andThat().compilerMessage().ofKindError(), line, column, text);
        }

        public BlackBoxTestFileAssertionsAnd hasNoteContaining(int line, int column, String... text) {
            return hasMessageContaining(expectThat.andThat().compilerMessage().ofKindNote(), line, column, text);
        }

        private BlackBoxTestFileAssertionsAnd hasMessageContaining(CompilerMessageCheckComparisonType check, int line, int column, String... text) {
            return new BlackBoxTestFileAssertionsAnd(check
                    .atSource(source)
                    .atLine(line)
                    .atColumn(column)
                    .contains(text), source, this.assertions);
        }

        public BlackBoxTestFileAssertionsAnd hasWarnings(int expected) {
            this.assertions.add(it -> {
                var warnings = it.getCompilerMessages().stream()
                        .filter(message -> (message.getKind() == Kind.WARNING || message.getKind() == Kind.MANDATORY_WARNING) && this.source.equals(message.getSource()))
                        .toList();

                assertThat(warnings).as("%s has %d warnings", source, expected).hasSize(expected);
            });

            return this;
        }

        public BlackBoxTestFileAssertionsAnd hasErrors(int expected) {
            this.assertions.add(it -> {
                var errors = it.getCompilerMessages().stream()
                        .filter(message -> message.getKind() == Kind.ERROR && this.source.equals(message.getSource()))
                        .toList();

                assertThat(errors).as("%s has", source, expected).hasSize(expected);
            });

            return this;
        }

        public BlackBoxTestFileAssertionsAnd hasNotes(int expected) {
            this.assertions.add(it -> {
                var errors = it.getCompilerMessages().stream()
                        .filter(message -> message.getKind() == Kind.NOTE && this.source.equals(message.getSource()))
                        .toList();

                assertThat(errors).as("%s has %d notes", source, errors).hasSize(expected);
            });

            return this;
        }

        public BlackBoxTestFileAssertionsAnd hasNoWarnings() {
            return hasWarnings(0);
        }

        public BlackBoxTestFileAssertionsAnd hasNoErrors() {
            return hasErrors(0);
        }

        public BlackBoxTestFileAssertionsAnd hasNoNotes() {
            return hasNotes(0);
        }

    }

    public static final class BlackBoxCustomAssertions {

        private final DoCustomAssertions assertions;

        private BlackBoxCustomAssertions(DoCustomAssertions assertions, List<CustomAssertion> list) {
            this.assertions = assertions;

            if (!list.isEmpty()) {
                this.assertions.executeCustomAssertions(it -> {
                    for (var assertion : list) {
                        assertion.executeCustomAssertions(it);
                    }
                });
            }
        }

        public BlackBoxCustomAssertions customAssertions(CustomAssertion customAssertion) {
            this.assertions.executeCustomAssertions(customAssertion);
            return this;
        }

    }

}
