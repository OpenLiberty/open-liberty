/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.ConversionException;
import com.ibm.ws.microprofile.config.interfaces.ConverterNotFoundException;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public abstract class AbstractConfig implements WebSphereConfig {

    private static final TraceComponent tc = Tr.register(AbstractConfig.class);

    private final SortedSources sources;
    private boolean closed = false;

    private final ConversionManager conversionManager;

    /**
     * The sources passed in should have already been wrapped up as an unmodifiable copy
     *
     * @param sources
     * @param converters
     * @param executor
     */
    public AbstractConfig(SortedSources sources, ConversionManager conversionManager) {
        this.sources = sources;
        this.conversionManager = conversionManager;
    }

    /**
     * @param <T>
     * @param propertyName
     * @param propertyType
     * @return
     */
    protected abstract Object getTypedValue(String propertyName, Type propertyType);

    /**
     * @return
     */
    protected abstract Set<String> getKeySet();

    /** {@inheritDoc} */
    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return sources;
    }

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        assertNotClosed();
        Optional<T> optional = null;
        try {
            T value = (T) getTypedValue(propertyName, propertyType);
            optional = Optional.ofNullable(value);
        } catch (ConverterNotFoundException | ConversionException e) {
            throw new IllegalArgumentException(e);
        }
        return optional;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        assertNotClosed();
        Set<String> keys = getKeySet();
        return keys;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
        }
    }

    /**
     * Throws ConfigException if this is closed
     */
    protected void assertNotClosed() {
        if (this.closed) {
            throw new IllegalStateException(Tr.formatMessage(tc, "config.closed.CWMCG0001E"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        assertNotClosed();
        StringBuilder sb = new StringBuilder();
        sb.append("Config ");
        sb.append(hashCode());
        sb.append("[");
        sb.append(getPropertyNames().size());
        sb.append(" keys from ");
        sb.append(sources.size());
        sb.append(" sources] : ");
        sb.append(sources);
        return sb.toString();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        T value = (T) getValue(propertyName, propertyType, false);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object getValue(String propertyName, Type propertyType) {
        Object value = getValue(propertyName, propertyType, false);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object getValue(String propertyName, Type propertyType, boolean optional) {
        Object value = null;
        assertNotClosed();
        try {
            if (getKeySet().contains(propertyName)) {
                value = getTypedValue(propertyName, propertyType);
            } else {
                if (optional) {
                    value = convertValue(ConfigProperty.UNCONFIGURED_VALUE, propertyType);
                } else {
                    throw new NoSuchElementException(Tr.formatMessage(tc, "no.such.element.CWMCG0015E", propertyName));
                }
            }
        } catch (ConverterNotFoundException | ConversionException e) {
            throw new IllegalArgumentException(e);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object getValue(String propertyName, Type propertyType, String defaultString) {
        Object value = null;
        assertNotClosed();
        try {
            if (getKeySet().contains(propertyName)) {
                value = getTypedValue(propertyName, propertyType);
            } else {
                value = convertValue(defaultString, propertyType);
            }
        } catch (ConverterNotFoundException | ConversionException e) {
            throw new IllegalArgumentException(e);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object convertValue(String rawValue, Type type) {
        assertNotClosed();
        return conversionManager.convert(rawValue, type);
    }

}
