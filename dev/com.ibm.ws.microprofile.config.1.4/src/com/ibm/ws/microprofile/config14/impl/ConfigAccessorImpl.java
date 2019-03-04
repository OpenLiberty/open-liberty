/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigSnapshot;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14.PropertyChangeListener;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfigAccessor;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfigSnapshot;

public class ConfigAccessorImpl<T> implements WebSphereConfigAccessor<T>, PropertyChangeListener {

    private static final TraceComponent tc = Tr.register(ConfigAccessorImpl.class);

    private final WebSphereConfig14 config;
    private final List<String> propertyNames;
    private final Class<T> rawType;
    private final Class<?> genericSubType;

    private final Duration cacheFor;
    private boolean evaluateVariables;
    private final Object defaultValue;
    private final Converter<T> converter;

    //must only be referenced within synchronized(this) block
    private boolean cacheValid = false;
    private long cacheExpiryTime;
    private SourcedValue cachedValue;
    private String mostRecentPropertyName;
    //*****************

    public ConfigAccessorImpl(WebSphereConfig14 config, List<String> propertyNames, Class<T> rawType, Class<?> genericSubType, Duration cacheFor,
                              boolean evaluateVariables, Object defaultValue, String defaultString, Converter<T> converter) {
        this.config = config;
        this.propertyNames = propertyNames;
        this.rawType = rawType;
        this.genericSubType = genericSubType;
        this.evaluateVariables = evaluateVariables;
        this.cacheFor = cacheFor;
        this.evaluateVariables = evaluateVariables;
        this.converter = converter;

        if (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
            if (!ConfigProperty.UNCONFIGURED_VALUE.equals(defaultString)) {
                if (ConfigProperty.NULL_VALUE.equals(defaultString)) {
                    this.defaultValue = config.convertValue(null, rawType, genericSubType);
                } else {
                    this.defaultValue = config.convertValue(defaultString, rawType, genericSubType);
                }
            } else {
                this.defaultValue = ConfigProperty.UNCONFIGURED_VALUE;
            }
        } else {
            if (ConfigProperty.NULL_VALUE.equals(defaultValue)) {
                this.defaultValue = null;
            } else {
                this.defaultValue = defaultValue;
            }
        }

        if (cacheFor != null) {
            getSourcedValue(); //this will have the effect of retrieving, converting and caching the value for use later
        }
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() {
        T value = getValue(null);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue(ConfigSnapshot snapshot) {
        SourcedValue sourcedValue = getSourcedValue((WebSphereConfigSnapshot) snapshot);

        if (sourcedValue == null) {
            throw new NoSuchElementException(Tr.formatMessage(tc, "no.such.element.CWMCG0015E", getPropertyName()));
        }
        @SuppressWarnings("unchecked")
        T value = (T) sourcedValue.getValue();

        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<T> getOptionalValue() {
        Optional<T> optValue = getOptionalValue(null);
        return optValue;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> getOptionalValue(ConfigSnapshot snapshot) {
        SourcedValue sourcedValue = getSourcedValue((WebSphereConfigSnapshot) snapshot);
        T value = null;
        if (sourcedValue != null) {
            value = (T) sourcedValue.getValue();
        }
        Optional<T> optValue = Optional.ofNullable(value);

        return optValue;
    }

    private SourcedValue getSourcedValue(WebSphereConfigSnapshot snapshot) {
        SourcedValue sourcedValue = null;
        if (snapshot == null) {
            synchronized (this) {
                if (!checkCache()) {
                    sourcedValue = config.getSourcedValue(this.propertyNames, this.rawType, this.genericSubType, this.defaultValue, this.evaluateVariables,
                                                          this.converter);

                    //only set the cache to valid if cacheFor is positive
                    if (this.cacheFor != null) {
                        this.cachedValue = sourcedValue;
                        this.cacheExpiryTime = System.nanoTime() + cacheFor.toNanos();
                        this.cacheValid = true;
                    }
                    if (sourcedValue != null) {
                        this.mostRecentPropertyName = sourcedValue.getKey();
                        config.registerPropertyChangeListener(this, this.mostRecentPropertyName);
                    }
                } else {
                    sourcedValue = this.cachedValue;
                }
            }
        } else {
            sourcedValue = snapshot.getSourcedValue(getPropertyName());
        }
        return sourcedValue;
    }

    @Override
    public SourcedValue getSourcedValue() {
        SourcedValue value = getSourcedValue(null);
        return value;
    }

    //must be called within synchronized(this) block
    private boolean checkCache() {
        if (this.cacheValid) {
            long now = System.nanoTime();
            if ((now - this.cacheExpiryTime) > 0) {
                this.cacheValid = false;
            }
        }
        return this.cacheValid;
    }

    /** {@inheritDoc} */
    @Override
    public String getPropertyName() {
        return propertyNames.get(propertyNames.size() - 1); //last one is always the base
    }

//    /** {@inheritDoc} */
//    @Override
//    public String getResolvedPropertyName() {
//        String resolvedPropertyName;
//        synchronized (this) {
//            resolvedPropertyName = mostRecentPropertyName;
//        }
//        return resolvedPropertyName;
//    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public T getDefaultValue() {
        if (this.defaultValue == null || ConfigProperty.NULL_VALUE.equals(this.defaultValue) || ConfigProperty.UNCONFIGURED_VALUE.equals(this.defaultValue)) {
            return null;
        } else {
            return (T) this.defaultValue;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPropertyChanged() {
        synchronized (this) {
            this.cacheValid = false;
        }
    }
}
