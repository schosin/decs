package de.schosin.decs.codegen;

import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.Proxy;
import java.util.Map;

public sealed interface BuildEnvironment {

    static BuildEnvironment determine(ProcessingEnvironment env) {
        var className = env.getClass().getName();

        for (var entry : KnownEnv.PREFIXES.entrySet()) {
            if (className.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        var handler = Proxy.isProxyClass(env.getClass()) ? Proxy.getInvocationHandler(env) : null;
        var handlerClassName = handler != null ? handler.getClass().getName() : null;

        if (handlerClassName != null) {
            for (var entry : KnownEnv.PREFIXES.entrySet()) {
                if (handlerClassName.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return new UnknownEnv(className, handlerClassName);
    }

    boolean generateFirstRound();

}

enum KnownEnv implements BuildEnvironment {

    JAVAC(true),
    ECLIPSE_JDT(true), // false works as well with 4.37, but let's try this
    INTELLIJ(true);

    static final Map<String, BuildEnvironment> PREFIXES = Map.of(
            "com.sun.tools.javac.", JAVAC,
            "org.eclipse.jdt.", ECLIPSE_JDT,
            "org.jetbrains.", INTELLIJ
    );

    private final boolean generateFirstRound;

    KnownEnv(boolean generateFirstRound) {
        this.generateFirstRound = generateFirstRound;
    }

    @Override
    public boolean generateFirstRound() {
        return generateFirstRound;
    }

}

record UnknownEnv(String processingEnv, String proxyHandler) implements BuildEnvironment {

    @Override
    public boolean generateFirstRound() {
        return true;
    }

}
