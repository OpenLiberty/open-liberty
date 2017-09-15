/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class stores a set of directed, many-to-many relationships.
 * Each key maps to many values (and each value may be mapped to by many keys).
 * 
 * @param <K> the Key class
 * @param <V> the Value class
 */
@SuppressWarnings("serial")
@Trivial
public class MultiMap<K, V> implements Cloneable {

    @Trivial
    private static final class SimpleMultiMap<X, Y> extends HashMap<X, Set<Y>> {

        @Trivial
        private final class ValueSet extends HashSet<Y> {
            private final Object key;
            boolean addedToMap;

            private ValueSet(Object key) {
                this.key = key;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean add(Y value) {
                if (!!!addedToMap) {
                    SimpleMultiMap.this.put((X) key, this);
                    addedToMap = true;
                }
                return super.add(value);
            }

            @Override
            public boolean remove(Object o) {
                if (super.remove(o)) {
                    if (super.isEmpty()) {
                        SimpleMultiMap.this.remove(key);
                        addedToMap = false;
                    }
                    return true;
                }
                return false;
            }
        }

        @Override
        public Set<Y> get(final Object key) {
            return containsKey(key) ? super.get(key) : new ValueSet(key);
        }
    }

    private final Map<K, Set<V>> map = new SimpleMultiMap<K, V>();

    private final Map<V, Set<K>> reverseMap = new SimpleMultiMap<V, K>();

    protected final String representation;

    public MultiMap() {
        this(" -> ");
    }

    public MultiMap(String representation) {
        this.representation = representation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        return (o instanceof MultiMap) && map.equals(((MultiMap<K, V>) o).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public void clear() {
        map.clear();
        reverseMap.clear();
    }

    protected boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public boolean containsValue(V value) {
        return reverseMap.containsKey(value);
    }

    public Set<V> get(K key) {
        return Collections.unmodifiableSet(map.get(key));
    }

    public Set<V> get(Iterable<? extends K> keys) {
        SortedSet<V> result = new TreeSet<V>();
        for (K key : keys)
            result.addAll(get(key));
        return Collections.unmodifiableSet(result);
    }

    public Set<K> keys(V value) {
        return Collections.unmodifiableSet(reverseMap.get(value));
    }

    public Set<K> keys(Iterable<? extends V> values) {
        Set<K> result = new HashSet<K>();
        for (V v : values)
            result.addAll(keys(v));
        return Collections.unmodifiableSet(result);
    }

    public Set<K> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public Set<V> values() {
        return Collections.unmodifiableSet(reverseMap.keySet());
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean add(K key, V value) {
        return map.get(key).add(value) && reverseMap.get(value).add(key);
    }

    public boolean remove(K key, V value) {
        return map.containsKey(key) && map.get(key).remove(value) && reverseMap.get(value).remove(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MultiMap<K, V> clone() {
        MultiMap<K, V> result;
        try {
            result = this.getClass().getConstructor(String.class).newInstance(representation);
            for (K k : map.keySet())
                for (V v : map.get(k))
                    result.add(k, v);
            return result;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public String toString() {
        if (map.isEmpty())
            return "{}";
        StringBuilder sb = new StringBuilder("{ \n");
        for (Entry<K, Set<V>> e : map.entrySet())
            for (V v : e.getValue())
                sb.append(String.format("    %s%s%s,%n", e.getKey(), representation, v));
        sb.setLength(sb.lastIndexOf(","));
        sb.append(String.format("%n}"));
        return sb.toString();
    }
}
