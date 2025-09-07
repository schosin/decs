package de.schosin.decs.codegen.system.helper;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public record GeneratorResult(AtomicBoolean requiresWorld, List<FieldSpec> fields, List<MethodSpec> methods, List<TypeSpec> types, CodeBlock.Builder fieldInit, CodeBlock.Builder offerArchetypeInit, CodeBlock.Builder runInit) {

    public static final GeneratorResult EMPTY = new GeneratorResult(new AtomicBoolean(false), List.of(), List.of(), List.of(), CodeBlock.builder(), CodeBlock.builder(), CodeBlock.builder());

    public GeneratorResult() {
        this(new AtomicBoolean(false), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), CodeBlock.builder(), CodeBlock.builder(), CodeBlock.builder());
    }

}
