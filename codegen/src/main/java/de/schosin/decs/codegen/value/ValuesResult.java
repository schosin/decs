package de.schosin.decs.codegen.value;

import java.util.HashMap;
import java.util.Map;

public record ValuesResult(Map<String, ValueData> values) {

    public ValuesResult() {
        this(new HashMap<>());
    }

}
