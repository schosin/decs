package de.schosin.decs.codegen.utils.source;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import de.schosin.decs.codegen.utils.AbstractGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class FallbackSourceProvider extends SourceProvider {

    static FallbackSourceProvider create(ProcessingEnvironment env) {
        if (INSTANCE == null) {
            INSTANCE = new FallbackSourceProvider(env, resolveRoots(env));
        }

        return INSTANCE;
    }

    private static FallbackSourceProvider INSTANCE;

    private static final String MAVEN_MAIN = "target/generated-sources";
    private static final String MAVEN_TEST = "target/generated-sources";

    private static final String GRADLE_MAIN = "build/generated/sources/annotationProcessor/java/main";
    private static final String GRADLE_TEST = "build/generated/sources/annotationProcessor/java/test";

    private final Set<Path> roots;

    private FallbackSourceProvider(ProcessingEnvironment env, Set<Path> roots) {
        super(env);

        this.roots = roots;
    }

    @Override
    public Source resolveSource(ExecutableElement executable, AbstractGenerator generator) {
        if (roots.isEmpty()) {
            generator.printWarning("Inlining not available for '%s': No source roots detected or configured".formatted(executable), executable);
            return null;
        }

        var topLevelType = getTopLevelType(executable, generator);
        if (topLevelType == null) {
            generator.printWarning("Inlining not available for '%s': Failed to resolve top level type".formatted(executable), executable);
            return null;
        }

        var fileName = topLevelType.getQualifiedName().toString().replace(".", "/") + ".java";

        for (var root : roots) {
            var path = root.resolve(fileName);
            try {
                var parentSource = Files.readString(path, StandardCharsets.UTF_8);
                return getSource(executable, root, parentSource, generator);
            } catch (IOException ex) {
                // try next
            }
        }

        generator.printWarning("Inlining not available for '%s': Failed to read source of '%s'".formatted(executable, topLevelType), executable);
        return null;
    }

    private Source getSource(ExecutableElement executable, Path root, String parentSource, AbstractGenerator generator) {
        if (executable.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new IllegalArgumentException("%s is abstract".formatted(executable));
        }

        var facade = buildParserFacade(root, generator.processingEnv);
        var cu = StaticJavaParser.parse(parentSource);

        if (cu.getImports().stream().anyMatch(ImportDeclaration::isAsterisk)) {
            generator.printError("Cannot use '*' in imports.", executable);
            return null;
        }

        var imports = cu.getImports().stream()
                .filter(decl -> !decl.isStatic())
                .map(ImportDeclaration::getName)
                .map(Name::asString)
                .toList();

        var staticImports = cu.getImports().stream()
                .filter(ImportDeclaration::isStatic)
                .map(ImportDeclaration::getName)
                .map(Name::asString)
                .toList();

        var importLookup = cu.getImports().stream()
                .filter(decl -> !decl.isStatic() && !decl.isAsterisk())
                .collect(Collectors.toMap(FallbackSourceProvider::getSimpleName, ImportDeclaration::getNameAsString, (a, b) -> a));

        try {
            var matchingMethods = cu.findAll(MethodDeclaration.class).stream()
                    .filter(declaration -> matches(executable, declaration, importLookup, facade, generator))
                    .toList();

            if (matchingMethods.size() != 1) {
                generator.printWarning("Inlining not available for '%s': Found %d possible matches in source. Please report this issue with the system type containing this method included.".formatted(executable, matchingMethods.size()), executable);
                return null;
            }

            var source = matchingMethods.getFirst().getBody()
                    .map(BlockStmt::toString)
                    .orElse(null);

            if (source == null) {
                generator.printWarning("Inlining not available for '%s': Failed to match method to source".formatted(executable), executable);
                return null;
            }

            // Retrieve code from sourceMethod
            String code = source.stripIndent();
            if (code.startsWith("{") && code.endsWith("}")) {
                code = code.substring(1, code.length() - 1);
            }

            return new Source(code, imports, staticImports);
        } catch (Exception ex) {
            var writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));

            generator.printWarning("Inlining not available for '%s': Exception while parsing source code: %s%s%s".formatted(executable, ex.getMessage(), System.lineSeparator(), writer.toString()), executable);
            return null;
        }
    }

    private JavaParserFacade buildParserFacade(Path root, ProcessingEnvironment env) {
        var typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(root),
                new ClassLoaderTypeSolver(FallbackSourceProvider.class.getClassLoader()));

        return JavaParserFacade.get(typeSolver);
    }

    private static String getSimpleName(ImportDeclaration importDeclaration) {
        var fqcn = importDeclaration.getNameAsString();

        var split = fqcn.split("\\.");
        return split[split.length - 1];
    }

    private boolean matches(ExecutableElement executable, MethodDeclaration declaration, Map<String, String> imports, JavaParserFacade facade, AbstractGenerator generator) {
        if (!executable.getSimpleName().toString().equals(declaration.getNameAsString())) {
            return false;
        }

        var declarationFqcn = getFullyQualifiedClassName(declaration);
        if (declarationFqcn == null) {
            generator.printWarning("Declaring type: MethodDeclaration FQCN null", executable);
            return false;
        }

        var elementFqcn = ((TypeElement) executable.getEnclosingElement()).getQualifiedName().toString();
        if (!elementFqcn.equals(declarationFqcn)) {
            return false;
        }

        var parameters = executable.getParameters();
        var declarationParameters = declaration.getParameters();
        if (parameters.size() != declarationParameters.size()) {
            return false;
        }

        for (int i = 0, s = parameters.size(); i < s; i++) {
            var parameter = parameters.get(i);
            var declarationParameter = declarationParameters.get(i);

            try {
                var resolved = facade.convertToUsage(declarationParameter.getType());
                var actualType = resolved.describe();

                if (!parameter.asType().toString().equals(actualType)) {
                    return false;
                }
            } catch (UnsolvedSymbolException ex) {
                var fqcn = imports.get(declarationParameter.getType().toString());
                if (fqcn != null && fqcn.equals(parameter.asType().toString())) {
                    continue;
                }

                generator.printWarning("UnsolvedSymbolException: fqcn %s != %s?".formatted(fqcn, parameter.asType().toString()));

                if (declarationParameter.getType() instanceof ReferenceType referenceType) {
                    if (referenceType.getMetaModel() != null) {
                        var metaModel = referenceType.getMetaModel();
                        generator.printWarning("UnsolvedSymbolException: %s = %s?".formatted(metaModel.getQualifiedClassName(), parameter.asType().toString()));

                        return false;
                    }

                    generator.printWarning("UnsolvedSymbolException for '%s' (reference type, no MetaModel): %s".formatted(parameter, ex.getMessage()));
                    return false;
                }

                generator.printWarning("UnsolvedSymbolException for '%s' (no reference type): %s".formatted(parameter, ex.getMessage()));
                return false;
            }
        }

        return true;
    }


    private String getFullyQualifiedClassName(MethodDeclaration declaration) {
        Node current = declaration;
        var fqcn = new StringBuilder();

        while (current.hasParentNode()) {
            current = current.getParentNode().get();

            switch (current) {
                case ClassOrInterfaceDeclaration type -> {
                    if (!fqcn.isEmpty()) {
                        fqcn.insert(0, "."); // nested type
                    }

                    fqcn.insert(0, type.getNameAsString());
                }
                case CompilationUnit cu -> {
                    if (cu.getPackageDeclaration().isPresent()) {
                        fqcn.insert(0, cu.getPackageDeclaration().get().getNameAsString() + ".");
                    }
                }
                default -> {
                    return null;
                }
            }
        }

        return fqcn.toString();
    }

    private TypeElement getTopLevelType(Element element, AbstractGenerator generator) {
        var current = element;
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            var parent = current.getEnclosingElement();
            generator.printWarning("%s: %s / %s".formatted(element.getSimpleName(), current.getSimpleName(), parent != null ? parent.getSimpleName() : null));

            if (parent == null || parent.getKind() == ElementKind.PACKAGE) {
                if (current instanceof TypeElement type) {
                    generator.printWarning("Found: " + type);
                    return type;
                }

                break;
            }

            current = parent;
        }

        if (element instanceof TypeElement type) {
            generator.printWarning("Found: " + type);
            return type;
        }

        generator.printWarning("Found none");
        return null;
    }

    private static Set<Path> resolveRoots(ProcessingEnvironment env) {
        // TODO env.getOptions()

        try {
            var file = env.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", "SourceRootFinder");
            env.getMessager().printWarning("Source output: " + file.toUri());

            var path = Path.of(file.toUri());

            var mavenPath = findAncestor(path, MAVEN_MAIN);
            if (mavenPath == null) {
                mavenPath = findAncestor(path, MAVEN_TEST);
            }

            if (mavenPath != null) {
                return Set.of(mavenPath.resolve("src/main/java"), mavenPath.resolve("src/test/java"));
            }

            var gradlePath = findAncestor(path, GRADLE_MAIN);
            if (gradlePath == null) {
                gradlePath = findAncestor(path, GRADLE_TEST);
            }

            if (gradlePath != null) {
                return Set.of(gradlePath.resolve("src/main/java"), gradlePath.resolve("src/test/java"));
            }

            env.getMessager().printWarning("Failed to determine source roots based on source output: %s".formatted(path.toString()));
            return Set.of();
        } catch (IOException ex) {
            var writer = new StringWriter();
            ex.printStackTrace(new PrintWriter(writer));

            env.getMessager().printWarning("Failed to determine source roots: %s%s%s".formatted(ex.getMessage(), System.lineSeparator(), writer.toString()));
        }

        return Set.of();
    }

    private static Path findAncestor(Path path, String subPath) {
        var target = Path.of(subPath.replace("/", File.separator));
        var current = path.toAbsolutePath();

        while (current != null) {
            if (current.endsWith(target)) {
                for (int i = 0; i < target.getNameCount(); i++) {
                    current = current.getParent();
                }

                return current;
            }

            current = current.getParent();
        }

        return null;
    }

}
