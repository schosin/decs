package de.schosin.decs.codegen.components;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

public record ComponentsResult(Map<String, ComponentData> components, List<ComponentData> sortedComponents, Map<String, Integer> selects, Map<String, Integer> reads, Map<String, Integer> writes,
        Map<String, List<TypeElement>> simpleNames) {

    public ComponentsResult() {
        this(new TreeMap<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public void add(ComponentData data) {
        this.components.put(data.className().canonicalName(), data);
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
            this.selects.merge(type.toString(), 1, Integer::sum);
        }

        return result;
    }

    /**
     * Should be used when a component is read by user code, e.g. {@code @EntityProcessor}, {@code @Inserted} or {@code @Removed}.
     */
    public ComponentData getReadComponent(TypeMirror type) {
        var result = components.get(type.toString());
        if (result != null) {
            this.reads.merge(type.toString(), 1, Integer::sum);
        }

        return result;
    }

    /**
     * Should be used when a component is written by user code, e.g. {@code @Archetype} or {@code @Transmute}.
     */
    public ComponentData getWrittenComponent(TypeMirror type) {
        var result = components.get(type.toString());
        if (result != null) {
            this.writes.merge(type.toString(), 1, Integer::sum);
        }

        return result;
    }

    public int getSelects(ComponentData data) {
        return selects.getOrDefault(data.className().canonicalName(), 0);
    }

    public int getReads(ComponentData data) {
        return reads.getOrDefault(data.className().canonicalName(), 0);
    }

    public int getWrites(ComponentData data) {
        return writes.getOrDefault(data.className().canonicalName(), 0);
    }

}
