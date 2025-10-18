package de.schosin.decs.codegen.utils.source;

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import de.schosin.decs.codegen.utils.AbstractGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

final class JavacSourceProvider extends SourceProvider {

    private final Trees trees;

    public JavacSourceProvider(ProcessingEnvironment env, Trees trees) {
        super(env);

        this.trees = trees;
    }

    @Override
    public Source resolveSource(ExecutableElement executable, AbstractGenerator generator) {
        var path = trees.getPath(executable);
        if (path == null) {
            return null;
        }

        if (!(path.getLeaf() instanceof MethodTree methodTree)) {
            return null;
        }

        var body = methodTree.getBody();
        if (body == null) {
            return null;
        }

        var cu = path.getCompilationUnit();

        if (cu.getImports().stream().anyMatch(decl -> decl.getQualifiedIdentifier().toString().endsWith(".*"))) {
            generator.printError("Cannot use '*' in imports.", executable);
            return null;
        }

        var imports = cu.getImports().stream()
                .filter(decl -> !decl.isStatic())
                .map(ImportTree::getQualifiedIdentifier)
                .map(Tree::toString)
                .toList();

        var staticImports = cu.getImports().stream()
                .filter(ImportTree::isStatic)
                .map(ImportTree::getQualifiedIdentifier)
                .map(Tree::toString)
                .toList();

        return new Source(body.toString(), imports, staticImports);
    }

}
