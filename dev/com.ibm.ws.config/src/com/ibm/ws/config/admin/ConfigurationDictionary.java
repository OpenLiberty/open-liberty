/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
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

    /** Set of supported simple types */
    static private final List<Class<?>> simpleTypes = Arrays.asList(new Class<?>[] { String.class, Integer.class, Long.class, Float.class, Double.class,
                                                                                    Byte.class,
                                                                                    Short.class, Character.class, Boolean.class });

    /** Set of supported primitive array types */
    static private final List<Class<?>> primitiveArrayTypes = Arrays.asList(new Class<?>[] { long[].class, int[].class, short[].class, char[].class,
                                                                                            byte[].class,
                                                                                            double[].class, float[].class, boolean[].class });

    /** Set of supported simple array types */
    static private final List<Class<?>> simpleArrayTypes = Arrays.asList(new Class<?>[] { String[].class, Integer[].class, Long[].class, Float[].class,
                                                                                         Double[].class,
                                                                                         Byte[].class, Short[].class, Character[].class, Boolean[].class });
    /** Set of IBM extended types */
    static private final List<Class<?>> extendedTypes = Arrays.asList(new Class<?>[] { SerializableProtectedString.class, OnError.class });

    static final Comparator<String> CASE_INSENSITIVE = new CaseInsensitive();

    @Trivial
    static class CaseInsensitive implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 7962325242424955159L;

        @Override
        public int compare(String s1, String s2) {
            if (s1 == s2)
                return 0;
            return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
        }

    };

    protected final Map<String, Object> properties = Collections.synchronizedMap(new TreeMap<String, Object>(CASE_INSENSITIVE));

    public ConfigurationDictionary() {

    }

    @Trivial
    private class ValuesEnumeration<T> implements Enumeration<Object> {
        final Iterator<Object> valuesIterator = properties.values().iterator();

        @Override
        @Trivial
        public boolean hasMoreElements() {
            return valuesIterator.hasNext();
        }

        @Override
        @Trivial
        public Object nextElement() {
            return valuesIterator.next();
        }
    }

    @Override
    public Enumeration<Object> elements() {
        return new ValuesEnumeration<Object>();
    }

    @Override
    public Object get(Object key) {
        if (key == null)
            throw new NullPointerException();
        return properties.get(key);
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Trivial
    private class KeysEnumeration<T> implements Enumeration<String> {
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

    @Override
    public Object put(String key, Object value) {
        if (key == null || value == null)
            throw new NullPointerException();

        // Will throw an illegal argument exception if not a valid configuration property type
        validateValue(value);

        return properties.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        if (key == null)
            throw new NullPointerException();
        return properties.remove(key);
    }

    @Override
    public int size() {
        return properties.size();
    }

    private static void validateValue(Object value) {
        Class<?> clazz = value.getClass();

        // Is it in the set of simpleTypes 
        if (simpleTypes.contains(clazz))
            return;

        // Is it an array of primitives or simples or extended
        if (simpleArrayTypes.contains(clazz) || primitiveArrayTypes.contains(clazz) || extendedTypes.contains(clazz))
            return;

        // Is it a Collection of simpleTypes
        if (value instanceof Collection<?>) {
            Collection<?> valueCollection = (Collection<?>) value;
            for (Iterator<?> it = valueCollection.iterator(); it.hasNext();) {
                Class<?> containedClazz = it.next().getClass();
                if (!simpleTypes.contains(containedClazz)) {
                    throw new IllegalArgumentException(containedClazz.getName() + " in " + clazz.getName()); //$NON-NLS-1$
                }
            }
            return;
        }

        // IBM extension to support Maps
        if (value instanceof Map) {
            Map<?, ?> valueMap = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
                Class<?> keyClazz = entry.getKey().getClass();
                if (keyClazz != String.class) {
                    throw new IllegalArgumentException(keyClazz.getName() + " in " + clazz.getName()); //$NON-NLS-1$
                }
                Class<?> valueClazz = entry.getValue().getClass();
                if (!simpleTypes.contains(valueClazz)) {
                    throw new IllegalArgumentException(valueClazz.getName() + " in " + clazz.getName()); //$NON-NLS-1$
                }
            }
            return;
        }

        throw new IllegalArgumentException(clazz.getName());
    }

    public ConfigurationDictionary copy() {
        ConfigurationDictionary result = new ConfigurationDictionary();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value.getClass().isArray()) {
                int arrayLength = Array.getLength(value);
                Object copyOfArray = Array.newInstance(value.getClass().getComponentType(), arrayLength);
                System.arraycopy(value, 0, copyOfArray, 0, arrayLength);
                result.properties.put(key, copyOfArray);
            } else if (value instanceof Vector)
                result.properties.put(key, ((Vector<?>) value).clone());
            else
                result.properties.put(key, value);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('{');

        synchronized (properties) {
            for (Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Object> entry = it.next();

                builder.append(entry.getKey());
                builder.append('=');

                Object value = entry.getValue();
                if (value == null || !value.getClass().isArray()) {
                    builder.append(value);
                } else {
                    String name = value.getClass().getComponentType().getName();
                    builder.append(name, name.lastIndexOf('.') + 1, name.length()).append("[]{");
                    for (int i = 0, length = Array.getLength(value); i < length; i++) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(Array.get(value, i));
                    }
                    builder.append('}');
                }

                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
        }

        return builder.append('}').toString();
    }

    /**
     * @param filter
     * @return
     */
    public boolean matches(Filter filter) {
        return filter.matches(this.properties);
    }
}
