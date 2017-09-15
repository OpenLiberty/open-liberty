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
package com.ibm.ws.event.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Storage class that combines both the Dictionary and Map interfaces.
 * 
 * @param <K>
 * @param <V>
 */
@Trivial
public class MapDictionary<K, V> extends Dictionary<K, V> implements Map<K, V> {
    private Map<K, V> localMap;
    private boolean isReadOnly = false;

    /**
     * Constructor.
     */
    @Trivial
    public MapDictionary() {
        this.localMap = new HashMap<K, V>();
    }

    /**
     * Constructor with optional input properties to store.
     * 
     * @param source
     */
    public MapDictionary(Map<K, V> source) {
        this();
        if (source != null) {
            this.localMap.putAll(source);
        }
    }

    /**
     * Attempt to set the read-only flag of the storage. If this is already set to
     * read-only,
     * then the input flag is ignored.
     * 
     * @param flag
     */
    public void setReadOnly(boolean flag) {
        if (!isReadyOnly())
            this.isReadOnly = flag;
    }

    /**
     * Query whether the storage is flagged as read-only currently.
     * 
     * @return boolean
     */
    public boolean isReadyOnly() {
        return this.isReadOnly;
    }

    /*
     * @see java.util.Dictionary#elements()
     */
    @Override
    public Enumeration<V> elements() {
        // use values method for isReadOnly check
        return new DictionaryEnumeration<V>(this.values().iterator());
    }

    /*
     * @see java.util.Dictionary#keys()
     */
    @Override
    public Enumeration<K> keys() {
        // use keySet method for isReadOnly check
        return new DictionaryEnumeration<K>(this.keySet().iterator());
    }

    /*
     * @see java.util.Dictionary#get(java.lang.Object)
     */
    @Override
    public V get(Object key) {
        return this.localMap.get(key);
    }

    /*
     * @see java.util.Dictionary#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return this.localMap.isEmpty();
    }

    /*
     * @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public V put(K key, V value) {
        if (isReadyOnly())
            throw new UnsupportedOperationException("Can't add property to read-only dictionary");

        return this.localMap.put(key, value);
    }

    /*
     * @see java.util.Dictionary#remove(java.lang.Object)
     */
    @Override
    public V remove(Object key) {
        if (isReadyOnly())
            throw new UnsupportedOperationException("Can't remove property from read-only dictionary");

        return this.localMap.remove(key);
    }

    /*
     * @see java.util.Dictionary#size()
     */
    @Override
    public int size() {
        return this.localMap.size();
    }

    /*
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        if (isReadyOnly())
            throw new UnsupportedOperationException("Can't clear read-only dictionary");

        this.localMap.clear();
    }

    /*
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return this.localMap.containsKey(key);
    }

    /*
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return this.localMap.containsValue(value);
    }

    /*
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        final Set<Entry<K, V>> localSet = this.localMap.entrySet();

        if (isReadyOnly())
            return Collections.unmodifiableSet(localSet);

        return localSet;
    }

    /*
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet() {
        final Set<K> localSet = this.localMap.keySet();

        if (isReadyOnly())
            return Collections.unmodifiableSet(localSet);

        return localSet;
    }

    /*
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values() {
        final Collection<V> localValues = this.localMap.values();

        if (isReadyOnly())
            return Collections.unmodifiableCollection(localValues);

        return localValues;
    }

    /*
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (isReadyOnly())
            throw new UnsupportedOperationException("Can't add properties to read-only dictionary");

        this.localMap.putAll(map);
    }

    /**
     * Enumeration wrapper of an iterator.
     * 
     * @param <T>
     */
    static class DictionaryEnumeration<T> implements Enumeration<T> {
        private final Iterator<T> localIterator;

        public DictionaryEnumeration(Iterator<T> iterator) {
            this.localIterator = iterator;
        }

        /*
         * @see java.util.Enumeration#hasMoreElements()
         */
        @Override
        public boolean hasMoreElements() {
            return this.localIterator.hasNext();
        }

        /*
         * @see java.util.Enumeration#nextElement()
         */
        @Override
        public T nextElement() {
            return this.localIterator.next();
        }
    }
}
