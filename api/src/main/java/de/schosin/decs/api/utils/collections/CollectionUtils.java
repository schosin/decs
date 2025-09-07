package de.schosin.decs.api.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {

    @SafeVarargs
    public static <T> List<T> listOf(T... items) {
        List<T> list = new ArrayList<T>();
        for (T item : items) {
            list.add(item);
        }

        return Collections.unmodifiableList(list);
    }

    public static <T> List<T> listOf(Collection<? extends T> collection) {
        return Collections.unmodifiableList(new ArrayList<>(collection));
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        Set<T> list = new HashSet<T>();
        for (T item : items) {
            list.add(item);
        }

        return Collections.unmodifiableSet(list);
    }

    public static <T> Set<T> setOf(Collection<? extends T> collection) {
        return Collections.unmodifiableSet(new HashSet<>(collection));
    }

    public static <K, V> Map<K, V> mapOf(Map<K, V> map) {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

}
