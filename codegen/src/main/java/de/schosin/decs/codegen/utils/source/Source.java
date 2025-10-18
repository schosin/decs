package de.schosin.decs.codegen.utils.source;

import java.util.List;

public record Source(String source, List<String> imports, List<String> staticImports) {

    public static final Source EMPTY = new Source("", List.of(), List.of());

}
