package de.schosin.decs.codegen.system;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.TypeElement;

public record SystemsResult(Set<TypeData> types, Set<TypeElement> seenTypes, Set<TypeElement> seenSystems, Set<TypeElement> seenUtilities) {

    public SystemsResult() {
        this(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

}
