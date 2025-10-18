package de.schosin.decs.codegen.utils.source;

import com.sun.source.util.Trees;
import de.schosin.decs.codegen.utils.AbstractGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.regex.Pattern;

public sealed abstract class SourceProvider permits JavacSourceProvider, FallbackSourceProvider {

    private static final Pattern LEADING_BRACE = Pattern.compile("^\s*\\{");
    private static final Pattern TRAILING_BRACE = Pattern.compile("^}\\s*$");

    public static SourceProvider getInstance(ProcessingEnvironment env) {
        // javac: Trees API
        try {
            var trees = Trees.instance(env);
            return new JavacSourceProvider(env, trees);
        } catch (Exception ex) {
            // Trees not available, probably not javac
        }

        // Fallback: Resolve source file via java.nio.Path
        return FallbackSourceProvider.create(env);
    }

    private final ProcessingEnvironment env;

    SourceProvider(ProcessingEnvironment env) {
        this.env = env;
    }

    public final Source getSource(ExecutableElement executable, AbstractGenerator generator) {
        var source = resolveSource(executable, generator);
        if (source == null && !(this instanceof FallbackSourceProvider)) {
            var fallback = FallbackSourceProvider.create(env);
            source = fallback.resolveSource(executable, generator);
        }

        if (source == null) {
            return null;
        }

        var sourceCode = source.source();
        var lines = sourceCode.split("\\R");
        var start = 0;
        var end = lines.length;

        // Remove leading / trailing whitespace around method braces
        if (lines[start].trim().equals("{")) {
            start++;
        }

        if (lines[end - 1].trim().equals("}")) {
            end--;
        }

        // Remove leading and trailing blank lines
        while (start < end && lines[start].trim().isEmpty()) {
            start++;
        }

        while (end > start && lines[end - 1].trim().isEmpty()) {
            end--;
        }

        // Empty source code?
        if (start >= end) {
            return Source.EMPTY;
        }

        var builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                builder.append(System.lineSeparator());
            }

            builder.append(lines[i]);
        }

        sourceCode = builder.toString();
        sourceCode = sourceCode.stripIndent();

        return new Source(sourceCode, source.imports(), source.staticImports());
    }

    protected abstract Source resolveSource(ExecutableElement executable, AbstractGenerator generator);

}