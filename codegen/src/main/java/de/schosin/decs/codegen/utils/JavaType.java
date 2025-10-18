package de.schosin.decs.codegen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;

import java.util.List;

public interface JavaType {

    String packageName();

    TypeSpec type();

    List<StaticImport> staticImports();

    default ClassName className() {
        return ClassName.get(packageName(), type().name());
    }

    static JavaType create(String packageName, TypeSpec type) {
        return create(packageName, type, List.of());
    }

    static JavaType create(String packageName, TypeSpec type, List<StaticImport> staticImports) {
        return new JavaTypeImpl(packageName, type, staticImports);
    }

    public record StaticImport(ClassName className, String[] names) {
    }

}

record JavaTypeImpl(String packageName, TypeSpec type, List<StaticImport> staticImports) implements JavaType {
}