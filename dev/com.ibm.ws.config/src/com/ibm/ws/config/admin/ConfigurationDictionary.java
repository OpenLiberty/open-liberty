/*******************************************************************************
 * Copyright (c) 2013,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.osgi.framework.Filter;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

@Trivial
public class ConfigurationDictionary extends Dictionary<String, Object> implements Serializable {
    private static final long serialVersionUID = 7966152868712543805L;

    @Trivial
    private static final boolean isSimpleType(Class<?> aType) {
        return ((aType == String.class) ||
                (aType == Integer.class) ||
                (aType == Long.class) ||
                (aType == Float.class) ||
                (aType == Double.class) ||
                (aType == Byte.class) ||
                (aType == Short.class) ||
                (aType == Character.class) ||
                (aType == Boolean.class));
    }

    @Trivial
    private static final boolean isPrimitiveArrayType(Class<?> aType) {
        return ((aType == long[].class) ||
                (aType == int[].class) ||
                (aType == short[].class) ||
                (aType == char[].class) ||
                (aType == byte[].class) ||
                (aType == double[].class) ||
                (aType == float[].class) ||
                (aType == boolean[].class));
    }

    @Trivial
    private static final boolean isSimpleArrayType(Class<?> aType) {
        return ((aType == String[].class) ||
                (aType == Integer[].class) ||
                (aType == Long[].class) ||
                (aType == Float[].class) ||
                (aType == Double[].class) ||
                (aType == Byte[].class) ||
                (aType == Short[].class) ||
                (aType == Character[].class) ||
                (aType == Boolean[].class));
    }

    @Trivial
    private static final boolean isExtendedType(Class<?> aType) {
        return ((aType == SerializableProtectedString.class) ||
                (aType == OnError.class));
    }

    @Trivial
    private static final void validateValue(Object value) {
        Class<?> clazz = value.getClass();

        if (isSimpleType(clazz) || isSimpleArrayType(clazz) || isPrimitiveArrayType(clazz) || isExtendedType(clazz)) {
            return;
        }

        // Do NOT test against Collection.class; the value class is an implementer of Collection.
        if (value instanceof Collection) {
            Collection<?> valueCollection = (Collection<?>) value;
            for (Object valueElement : valueCollection) {
                Class<?> containedClazz = valueElement.getClass();
                if (!isSimpleType(containedClazz)) {
                    throw new IllegalArgumentException(containedClazz.getName() + " in " + clazz.getName());
                }
            }
            return;
        }

        // Do NOT test against Map.class; the value class is an implementer of Map.
        if (value instanceof Map) {
            Map<?, ?> valueMap = (Map<?, ?>) value;
            valueMap.forEach((Object key, Object valueElement) -> {
                Class<?> keyClazz = key.getClass();
                if (keyClazz != String.class) {
                    throw new IllegalArgumentException(keyClazz.getName() + " in " + clazz.getName());
                }
                Class<?> elementClazz = valueElement.getClass();
                if (!isSimpleType(elementClazz)) {
                    throw new IllegalArgumentException(elementClazz.getName() + " in " + clazz.getName());
                }
            });
            return;
        }

        throw new IllegalArgumentException(clazz.getName());
    }

    @Trivial
    private static final Object copyValue(Object value) {
        if (value.getClass().isArray()) {
            int arrayLength = Array.getLength(value);
            Object copyOfArray = Array.newInstance(value.getClass().getComponentType(), arrayLength);
            System.arraycopy(value, 0, copyOfArray, 0, arrayLength);
            return copyOfArray;
        } else if (value instanceof Vector) {
            return ((Vector<?>) value).clone();
        } else {
            return value;
        }
    }

    //

    @Trivial
    private static TreeMap<String, Object> newProperties() {
        return new TreeMap<String, Object>((String s1, String s2) -> s1.compareToIgnoreCase(s2));
    }

    @Trivial
    public ConfigurationDictionary() {
        this(newProperties());
    }

    @Trivial
    private ConfigurationDictionary(TreeMap<String, Object> rawProperties) {
        this.properties = Collections.synchronizedMap(rawProperties);
    }

    public ConfigurationDictionary copy() {
        TreeMap<String, Object> rawProperties = newProperties();

        synchronized (properties) { // 'forEach' is not synchronized.
            properties.forEach((String key, Object value) -> {
                rawProperties.put(key, copyValue(value));
            });
        }

        return new ConfigurationDictionary(rawProperties);
    }

    //

    // TODO: Map iterators are not thread safe: Having the
    //       map be a synchronized map does not provide complete
    //       thread safety.
    //
    //       See the comment on 'Collections.synchronizedMap(Map<K, V>)'.

    protected final Map<String, Object> properties;

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new NullPointerException();
        } else {
            return properties.get(key); // thread safe
        }
    }

    @Override
    public Object put(String key, Object value) {
        if ((key == null) || (value == null)) {
            throw new NullPointerException();
        }

        validateValue(value); // throws IllegalArgumentException

        return properties.put(key, value); // thread safe
    }

    @Override
    public Object remove(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return properties.remove(key); // thread safe
    }

    @Override
    public int size() {
        return properties.size(); // thread safe
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty(); // thread safe
    }

    public boolean matches(Filter filter) {
        // TODO: Should this synchronize on the properties?
        return filter.matches(properties);
    }

    @Trivial
    private class ValuesEnumeration<T> implements Enumeration<Object> {
        // TODO: The values iterator is not thread safe:
        //       use of the values enumeration should synchronize on 'properties'.
        //       See the comment on 'Collections.synchronizedMap(Map<K, V>)'.
        final Iterator<Object> valuesIterator = properties.values().iterator();

        @Override
        public boolean hasMoreElements() {
            return valuesIterator.hasNext();
        }

        @Override
        public Object nextElement() {
            return valuesIterator.next();
        }
    }

    @Override
    public Enumeration<Object> elements() {
        return new ValuesEnumeration<Object>();
    }

    @Trivial
    private class KeysEnumeration<T> implements Enumeration<String> {
        // TODO: The keys iterator is not thread safe:
        //       use of the keys enumeration should synchronize on 'properties'.
        //       See the comment on 'Collections.synchronizedMap(Map<K, V>)'.

        Iterator<String> keysIterator = properties.keySet().iterator();

        @Override
        @Trivial
        public boolean hasMoreElements() {
            return keysIterator.hasNext();
        }

        @Override
        @Trivial
        public String nextElement() {
            return keysIterator.next();
        }
    }

    @Override
    public Enumeration<String> keys() {
        return new KeysEnumeration<String>();
    }

    //

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append('{');

        synchronized (properties) { // 'forEach' is not synchronized.
            properties.forEach((String key, Object value) -> {
                if (builder.length() != 0) {
                    builder.append(", ");
                }

                builder.append(key);
                builder.append('=');

                if ((value == null) || !value.getClass().isArray()) {
                    builder.append(value);

                } else {
                    String name = value.getClass().getComponentType().getName();
                    builder.append(name, name.lastIndexOf('.') + 1, name.length());
                    builder.append("[]{");
                    for (int i = 0, length = Array.getLength(value); i < length; i++) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(Array.get(value, i));
                    }
                    builder.append('}');
                }
            });
        }

        builder.append('}');

        return builder.toString();
    }
}
