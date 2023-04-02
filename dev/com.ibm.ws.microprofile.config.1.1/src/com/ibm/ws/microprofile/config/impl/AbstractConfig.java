/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config.impl;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
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
    @Trivial
    public AbstractConfig(ConversionManager conversionManager, SortedSources sources) {
        this.sources = sources;
        this.conversionManager = conversionManager;
    }

    /**
     * @return
     */
    protected abstract Set<String> getKeySet();

    /** {@inheritDoc} */
    @Override
    public Iterable<ConfigSource> getConfigSources() {
        assertNotClosed();
        return this.sources;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        assertNotClosed();
        SourcedValue sourced = getSourcedValue(propertyName, propertyType);
        T value = null;
        if (sourced != null) {
            value = (T) sourced.getValue();
        }
        Optional<T> optional = Optional.ofNullable(value);
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

    protected boolean isClosed() {
        return this.closed;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Config[");
        sb.append(hashCode());
        sb.append("](");

        if (this.closed) {
            sb.append("CLOSED");
        } else {
            sb.append(this.sources.size());
            sb.append(" sources");
        }
        sb.append(")");
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

        SourcedValue sourced = getSourcedValue(propertyName, propertyType);
        if (sourced != null) {
            value = sourced.getValue();
        } else {
            if (!optional) {
                throw new NoSuchElementException(Tr.formatMessage(tc, "no.such.element.CWMCG0015E", propertyName));
            }
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object getValue(String propertyName, Type propertyType, String defaultString) {
        Object value = null;
        assertNotClosed();

        SourcedValue sourced = getSourcedValue(propertyName, propertyType);
        if (sourced != null) {
            value = sourced.getValue();
        } else {
            if (!defaultString.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                value = convertValue(defaultString, propertyType);
            } else {
                throw new NoSuchElementException(Tr.formatMessage(tc, "no.such.element.CWMCG0015E", propertyName));
            }
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public <T> T convertValue(String rawValue, Class<T> type) {
        assertNotClosed();
        @SuppressWarnings("unchecked")
        T value = (T) getConversionManager().convert(rawValue, type);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object convertValue(String rawValue, Type type) {
        assertNotClosed();
        Object value = getConversionManager().convert(rawValue, type);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object convertValue(String rawValue, Type type, Class<?> genericSubType) {
        assertNotClosed();
        Object value = getConversionManager().convert(rawValue, type, genericSubType);
        return value;
    }

    public ConversionManager getConversionManager() {
        return this.conversionManager;
    }

}
