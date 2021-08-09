/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Simple set of utilities for dealing with the Dictionary objects used by
 * the OSGi ConfigAdmin service.
 */
public class DictionaryUtils {
    /**
     * Return a Dictionary object backed by the original map (rather
     * than copying the elements from the map into a new dictionary).
     * 
     * @param map
     *            Map with String keys and Object values
     * 
     * @return Dictionary with String keys and Object values where all
     *         operations are delegated to the original map
     */
    public static Dictionary<String, Object> mapToDictionary(Map<String, Object> map) {
        return new MapWrapper(map);
    }

    /**
     * Delegating Dictionary: It wraps a Map with String keys and Object values
     * with a Dictionary. No value copying is performed.
     * 
     * @param map
     *            Map with String keys and Object values
     * 
     * @return Dictionary with String keys and Object values where all
     *         operations are delegated to the original map
     */
    static class MapWrapper extends Dictionary<String, Object> {
        private final Map<String, Object> myMap;

        MapWrapper(Map<String, Object> map) {
            myMap = map;
        }

        @Override
        public boolean isEmpty() {
            return myMap.isEmpty();
        }

        @Override
        public Enumeration<String> keys() {
            Set<String> keys = myMap.keySet();
            return new EnumerationWrapper<String>(keys.iterator());
        }

        @Override
        public Object put(String key, Object value) {
            return myMap.put(key, value);
        }

        @Override
        public int size() {
            return myMap.size();
        }

        @Override
        public Object get(Object key) {
            return myMap.get(key);
        }

        @Override
        public Object remove(Object key) {
            return myMap.remove(key);
        }

        @Override
        public Enumeration<Object> elements() {
            Collection<Object> values = myMap.values();
            return new EnumerationWrapper<Object>(values.iterator());
        }
    }

    /**
     * Enumeration that wraps/delegates to an associated map. This is used
     * by the {@link MapWrapper} to return enumerations for keys and values.
     * 
     * @param <E>
     *            The type of object to enumerate: will be String for keys,
     *            and Object for values
     */
    static class EnumerationWrapper<E> implements Enumeration<E> {
        Iterator<E> myI;

        EnumerationWrapper(Iterator<E> i) {
            myI = i;
        }

        @Override
        public boolean hasMoreElements() {
            return myI.hasNext();
        }

        @Override
        public E nextElement() {
            return myI.next();
        }
    }
}
