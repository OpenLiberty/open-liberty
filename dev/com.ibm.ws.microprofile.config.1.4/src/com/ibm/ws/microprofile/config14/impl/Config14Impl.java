/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.impl.AbstractConfig;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSources;
import com.ibm.ws.microprofile.config.impl.SourcedValueImpl;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public class Config14Impl extends AbstractConfig implements WebSphereConfig {

    private final SortedSources sources;
    private final ConversionManager conversionManager;

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
        this.sources = sources;
        this.conversionManager = conversionManager;
    }

    /** {@inheritDoc} */
    @Override
    public SourcedValue getSourcedValue(String propertyName, Type propertyType) {
        SourcedValue sourcedValue = null;
        SourcedValue rawProp = getRawCompositeValue(propertyName);
        if (rawProp != null) {
            Object value = this.conversionManager.convert((String) rawProp.getValue(), propertyType);
            sourcedValue = new SourcedValueImpl(propertyName, value, propertyType, rawProp.getSource());
        }
        return sourcedValue;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getKeySet() {
        HashSet<String> result = new HashSet<>();

        for (ConfigSource source : sources) {
            Set<String> names = source.getPropertyNames();
            result.addAll(names);
        }

        return result;
    }

    /**
     * @param key
     * @return
     */
    @Trivial
    private SourcedValue getRawCompositeValue(String key) {
        SourcedValue raw = null;
        for (ConfigSource source : sources) {
            String value = source.getValue(key);
            if (value != null || source.getPropertyNames().contains(key)) {
                String sourceID = source.getName();
                raw = new SourcedValueImpl(key, value, String.class, sourceID);
                break;
            }
        }
        return raw;
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
            SourcedValue rawCompositeValue = getRawCompositeValue(key);
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

    /**
     * Get the converted value of the given property.
     * If the property is not found and optional is true then use the default string to create a value to return.
     * If the property is not found and optional is false then throw an exception.
     *
     * @param propertyName  the property to get
     * @param propertyType  the type to convert to
     * @param optional      is the property optional
     * @param defaultString the default string to use if the property was not found and optional is true
     * @return the converted value
     * @throws NoSuchElementException thrown if the property was not found and optional was false
     */
    @Override
    protected Object getValue(String propertyName, Type propertyType, boolean optional, String defaultString) {
        Object value = super.getValue(propertyName, propertyType, optional, defaultString);
        if (ConfigProperty.NULL_VALUE.equals(value)) {
            value = null;
        }
        return value;
    }
}
