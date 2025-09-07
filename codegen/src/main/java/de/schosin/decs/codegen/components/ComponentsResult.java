package de.schosin.decs.codegen.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public record ComponentsResult(Map<String, ComponentData> components, Map<String, Integer> selects, Map<String, Integer> reads, Map<String, Integer> writes,
        Map<String, List<TypeElement>> simpleNames) {

    public ComponentsResult() {
        this(new TreeMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public boolean isComponent(TypeMirror type) {
        return components.containsKey(type.toString());
    }

    /**
     * Should be used when a component is selected for compositions, e.g.{@code @All}, {@code @One} or {@code @None}.
     */
    public ComponentData getSelectedComponent(TypeMirror type) {
        var result = components.get(type.toString());
        if (result != null) {
            this.selects.merge(result.type().toString(), 1, Integer::sum);
        }

        return result;
    }

    /**
     * Should be used when a component is read by user code, e.g. {@code @EntityProcessor}, {@code @Inserted} or {@code @Removed}.
     */
    public ComponentData getReadComponent(TypeMirror type) {
        var result = components.get(type.toString());
        if (result != null) {
            this.reads.merge(result.type().toString(), 1, Integer::sum);
        }

        return result;
    }

    /**
     * Should be used when a component is written by user code, e.g. {@code @Archetype} or {@code @Transmute}.
     */
    public ComponentData getWrittenComponent(TypeMirror type) {
        var result = components.get(type.toString());
        if (result != null) {
            this.writes.merge(result.type().toString(), 1, Integer::sum);
        }

        return result;
    }

    public int getSelects(DeclaredType type) {
        return selects.getOrDefault(type.toString(), 0);
    }

    public int getReads(DeclaredType type) {
        return reads.getOrDefault(type.toString(), 0);
    }

    public int getWrites(DeclaredType type) {
        return writes.getOrDefault(type.toString(), 0);
    }

}
