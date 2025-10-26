package de.schosin.decs.codegen.system;

import com.palantir.javapoet.ClassName;
import de.schosin.decs.codegen.system.SystemGenerator.SystemJavaType;
import de.schosin.decs.codegen.system.TypeData.SystemData;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SystemsResult(Set<TypeData> types, Set<TypeElement> seenTypes, Set<TypeElement> seenSystems,
                            Set<TypeElement> seenUtilities, List<SystemJavaType> systemJavaTypes,
                            List<SystemJavaType> utilityJavaTypes) {

    public SystemsResult() {
        this(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new ArrayList<>(), new ArrayList<>());
    }

    public SystemData getSystem(TypeElement element) {
        return types.stream()
                .filter(type -> type instanceof SystemData && type.element().equals(element))
                .map(SystemData.class::cast)
                .findFirst()
                .orElse(null);
    }

    public SystemData getSystem(ClassName className) {
        return types.stream()
                .filter(type -> type instanceof SystemData && type.className().equals(className))
                .map(SystemData.class::cast)
                .findFirst()
                .orElse(null);
    }

}
