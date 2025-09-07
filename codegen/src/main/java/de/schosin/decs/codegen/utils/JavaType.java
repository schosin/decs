package de.schosin.decs.codegen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;

public interface JavaType {

    String packageName();

    TypeSpec type();

    default ClassName className() {
        return ClassName.get(packageName(), type().name());
    }

    static JavaType create(String packageName, TypeSpec type) {
        return new JavaTypeImpl(packageName, type);
    }

}

record JavaTypeImpl(String packageName, TypeSpec type) implements JavaType {
}