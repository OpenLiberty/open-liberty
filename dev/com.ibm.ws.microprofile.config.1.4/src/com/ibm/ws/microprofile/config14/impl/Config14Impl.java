/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.impl.AbstractConfig;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SourcedValueImpl;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.microprofile.config14.sources.AppPropertyConfig14Source;
import com.ibm.ws.microprofile.config14.sources.ConfigString;
import com.ibm.ws.microprofile.config14.sources.ExtendedConfigSource;

public class Config14Impl extends AbstractConfig implements WebSphereConfig {

    private final Map<String, TypeCache> convertedValueCache = new ConcurrentHashMap<>();
    private final TimedCache<String, SourcedValue> rawValueCache;

    /**
     * The sources passed in should have already been wrapped up as an unmodifiable copy
     *
     * @param conversionManager
     * @param sources
     * @param executor
     * @param refreshInterval
     */
    public Config14Impl(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        super(conversionManager, sources);
        rawValueCache = new TimedCache<>(executor, refreshInterval, TimeUnit.MILLISECONDS);
    }

    /** {@inheritDoc} */
    @Override
    public SourcedValue getSourcedValue(String propertyName, Type propertyType) {
        SourcedValue sourcedValue = null;
        SourcedValue rawValue = getCachedRawValue(propertyName);
        if (rawValue != null) {
            sourcedValue = getCachedSourcedValue(rawValue, propertyType);
        }
        return sourcedValue;
    }

    private SourcedValue getCachedSourcedValue(SourcedValue rawValue, Type propertyType) {
        SourcedValue value = null;
        String key = rawValue.getKey();
        TypeCache typeCache = convertedValueCache.get(key);
        if (typeCache == null || !rawValue.equals(typeCache.getRawValue())) {
            //if there is nothing in the cache or the raw value doesn't match, create a new entry
            typeCache = new TypeCache(rawValue);
            convertedValueCache.put(key, typeCache);
        }
        SourcedValue cachedValue = typeCache.getConvertedValues().get(propertyType);
        if (cachedValue == null) {
            //if there is no value found for the given type, convert the raw String and add it to the cache
            Object converted = getConversionManager().convert((String) rawValue.getValue(), propertyType);
            cachedValue = new SourcedValueImpl(key, converted, propertyType, rawValue.getSource());
            typeCache.getConvertedValues().put(propertyType, cachedValue);
        }
        value = cachedValue;
        return value;
    }

    @Trivial
    @FFDCIgnore(NullPointerException.class)
    private static Set<String> getPropertyNames(ConfigSource source) {
        Set<String> names = null;
        try {
            names = source.getPropertyNames();
        } catch (NullPointerException e) {
            names = Collections.emptySet();
        }
        return names;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getKeySet() {
        HashSet<String> result = new HashSet<>();

        for (ConfigSource source : getConfigSources()) {
            Set<String> names = getPropertyNames(source);
            result.addAll(names);
        }

        return result;
    }

    @Trivial
    private SourcedValue getCachedRawValue(String key) {
        return rawValueCache.get(key, this::getRawValue);
    }

    /**
     * @param key
     * @return
     */
    @Trivial
    private SourcedValue getRawValue(String key) {
        SourcedValue raw = null;
        for (ConfigSource source : getConfigSources()) {
            if (source instanceof ExtendedConfigSource) {
                // ExtendedConfigSource allows us to differentiate between null as a value and value not being present
                ConfigString configString = ((ExtendedConfigSource) source).getConfigString(key);
                if (configString.isPresent()) {
                    String sourceID = source.getName();
                    raw = new SourcedValueImpl(key, configString.getValue(), String.class, sourceID);
                    break;
                }
            } else {
                // For a standard config source, we have to check both getValue and then getPropertyNames
                // to tell the difference between the key not being present and the key being associated with a null value
                String value = source.getValue(key);
                if (value != null || source.getPropertyNames().contains(key)) {
                    String sourceID = source.getName();
                    raw = new SourcedValueImpl(key, value, String.class, sourceID);
                    break;
                }
            }
        }
        return raw;
    }

    @Override
    public void close() {
        rawValueCache.close();
        for (ConfigSource source : getConfigSources()) {
            if (source instanceof AppPropertyConfig14Source) {
                // Special case, AppPropertyConfig14Source needs to be closed :(
                ((AppPropertyConfig14Source) source).close();
            }
        }
        super.close();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String dump() {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = getKeySet();
        keys = new TreeSet<String>(keys);
        Iterator<String> keyItr = keys.iterator();
        while (keyItr.hasNext()) {
            String key = keyItr.next();
            SourcedValue rawCompositeValue = getCachedRawValue(key);
            if (rawCompositeValue == null) {
                sb.append("null");
            } else {
                sb.append(rawCompositeValue);
            }
            if (keyItr.hasNext()) {
                sb.append("\n");
            }
        }

        return sb.toString();

    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        throw new UnsupportedOperationException();
    }

}
