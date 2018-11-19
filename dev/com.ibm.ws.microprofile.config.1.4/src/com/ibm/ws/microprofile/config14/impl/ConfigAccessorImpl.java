/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigSnapshot;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.microprofile.config14.interfaces.Config14Constants;

public class ConfigAccessorImpl<T> implements ConfigAccessor<T> {

    private final String propertyName;
    private final Class<T> conversionType;
    private Converter<T> converter;
    private T defaultValue;
    private String defaultString;
    private long cacheFor;
    private TimeUnit cacheForUnit;
    private String suffixValue;
    private ConfigAccessor<String> suffixAccessor;
    private boolean evaluateVariables;
    private WebSphereConfig config;

    public ConfigAccessorImpl(WebSphereConfig config, String propertyName, Class<T> conversionType) {
        this(config, propertyName, conversionType, true); //evaluateVariables defaults to true
    }

    public ConfigAccessorImpl(WebSphereConfig config, String propertyName, Class<T> conversionType, boolean evaluateVariables) {
        this.config = config;
        this.propertyName = propertyName;
        this.conversionType = conversionType;
        this.evaluateVariables = evaluateVariables;
    }

//    /** {@inheritDoc} */
//    @Override
//    public <N> ConfigAccessor<N> as(Class<N> clazz) {
//        ConfigAccessor<N> accessor = new ConfigAccessorImpl<>(propertyName, clazz);
//        return accessor;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessor<List<T>> asList() {
//        ConfigAccessor<List<T>> accessor = new ConfigListAccessorImpl<>(propertyName, conversionType);
//        return accessor;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessor<Set<T>> asSet() {
//        ConfigAccessor<Set<T>> accessor = new ConfigSetAccessorImpl<>(propertyName, conversionType);
//        return accessor;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessor<T> useConverter(Converter<T> converter) {
//        this.converter = converter;
//        return this;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessor<T> withDefault(T value) {
//        this.defaultValue = value;
//        return this;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessor<T> withStringDefault(String value) {
//        this.defaultString = value;
//        return this;
//    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<T> cacheFor(long value, TimeUnit timeUnit) {
        this.cacheFor = value;
        this.cacheForUnit = timeUnit;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<T> evaluateVariables(boolean evaluateVariables) {
        this.evaluateVariables = evaluateVariables;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<T> addLookupSuffix(String suffixValue) {
        this.suffixValue = suffixValue;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<T> addLookupSuffix(ConfigAccessor<String> suffixAccessor) {
        this.suffixAccessor = suffixAccessor;
        return this;
    }

    public static String resolve(Config config, String raw) {
        String resolved = raw;
        //System.out.println("RAW: " + raw);
        if (resolved != null) {
            int evalStartIdx = resolved.indexOf(Config14Constants.EVAL_START_TOKEN);
            while (evalStartIdx > -1) {
                String part1 = resolved.substring(0, evalStartIdx);
                String part2 = resolved.substring(evalStartIdx);
                int evalEndIdx = part2.indexOf(Config14Constants.EVAL_END_TOKEN);
                if (evalEndIdx < 0) {
                    break; //can't find the end of the variable name so stop looking
                }
                String variableName = part2.substring(Config14Constants.EVAL_START_TOKEN.length(), evalEndIdx);
                //System.out.println("VAR: " + variableName);
                String value = config.getValue(variableName, String.class);
                //System.out.println("VAL: " + value);

                String part3 = part2.substring(evalEndIdx + Config14Constants.EVAL_END_TOKEN.length());
                String part3resolved = resolve(config, part3);

                //System.out.println("PT1: " + part1);
                //System.out.println("PT3: " + part3resolved);
                resolved = part1 + value + part3resolved;
                //System.out.println("RES: " + resolved);
                evalStartIdx = resolved.indexOf(Config14Constants.EVAL_START_TOKEN);
            }
        }
        return resolved;
    }

    public String getResolved(boolean optional) {
        String value = (String) this.config.getValue(this.propertyName, String.class, optional);
        if (this.evaluateVariables) {
            value = resolve(this.config, value);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() {
        String resolved = getResolved(false);
        T value = config.convertValue(resolved, this.conversionType);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue(ConfigSnapshot configSnapshot) {
        ConfigSnapshotImpl snapshotImpl = (ConfigSnapshotImpl) configSnapshot;
        String resolved = snapshotImpl.getResolvedValue(getPropertyName(), false);
        T value = config.convertValue(resolved, this.conversionType);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<T> getOptionalValue() {
        String resolved = getResolved(true);
        T value = null;
        if (resolved != null) {
            value = config.convertValue(resolved, this.conversionType);
        }

        Optional<T> optValue = Optional.ofNullable(value);

        return optValue;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<T> getOptionalValue(ConfigSnapshot configSnapshot) {
        ConfigSnapshotImpl snapshotImpl = (ConfigSnapshotImpl) configSnapshot;
        String resolved = snapshotImpl.getResolvedValue(getPropertyName(), true);
        T value = null;
        if (resolved != null) {
            value = config.convertValue(resolved, this.conversionType);
        }

        Optional<T> optValue = Optional.ofNullable(value);

        return optValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getPropertyName() {
        return this.propertyName;
    }
//
//    /** {@inheritDoc} */
//    @Override
//    public String getResolvedPropertyName() {
//        // TODO return this.propertyName + suffix
//        return null;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public T getDefaultValue() {
//        return this.defaultValue;
//    }
}
