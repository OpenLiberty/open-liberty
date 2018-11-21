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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config14.interfaces.Config14Constants;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;

public class ConfigAccessorBuilderImpl<T> implements ConfigAccessorBuilder<T> {

    private final WebSphereConfig14 config;
    private final String propertyName;
    private final Class<T> conversionType;
    private final List<String> suffixValues = new ArrayList<String>();

    private long cacheFor = 0;
    private ChronoUnit cacheForUnit = ChronoUnit.MILLIS;
    private boolean evaluateVariables = Config14Constants.EVALUATE_VARIABLES_DEFAULT;
    private Object defaultValue = ConfigProperty.UNCONFIGURED_VALUE;
    private String defaultString = ConfigProperty.UNCONFIGURED_VALUE;
    private Converter<T> converter = null;

    public ConfigAccessorBuilderImpl(WebSphereConfig14 config, String propertyName, Class<T> conversionType) {
        this.config = config;
        this.propertyName = propertyName;
        this.conversionType = conversionType;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> withConverter(Converter<T> converter) {
        if (converter == null) {
            //TODO NLS
            throw new IllegalArgumentException("converter may not be null");
        }
        synchronized (this) {
            this.converter = converter;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> withDefault(T value) {
        synchronized (this) {
            this.defaultValue = value;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> withStringDefault(String value) {
        synchronized (this) {
            this.defaultString = value;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> cacheFor(long value, ChronoUnit timeUnit) {
        if (timeUnit == null) {
            //TODO NLS
            throw new IllegalArgumentException("timeUnit may not be null");
        }
        if (value < 0) {
            //TODO NLS
            throw new IllegalArgumentException("cacheFor value must be non-negative");
        }
        synchronized (this) {
            this.cacheFor = value;
            this.cacheForUnit = timeUnit;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> evaluateVariables(boolean evaluateVariables) {
        synchronized (this) {
            this.evaluateVariables = evaluateVariables;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> addLookupSuffix(String suffixValue) {
        if (suffixValue == null) {
            //TODO NLS
            throw new IllegalArgumentException("suffixValue may not be null");
        }
        synchronized (this) {
            this.suffixValues.add(suffixValue);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<T> build() {
        ConfigAccessor<T> accessor;
        synchronized (this) {
            Duration cacheForDuration = null;
            if (cacheFor > 0) {
                cacheForDuration = Duration.of(cacheFor, cacheForUnit);
            }

            List<String> propertyNames = generatePropertyNameList(this.propertyName, this.suffixValues);

            accessor = new ConfigAccessorImpl<T>(this.config, propertyNames, this.conversionType, cacheForDuration, this.evaluateVariables, this.defaultValue, this.defaultString, this.converter);
        }
        return accessor;
    }

    /**
     * This method uses a binary count down to append suffixes to a base propertyName.
     *
     * e.g.
     * if there are 4 suffixes then the counter starts at 15 (1111) which would mean that all of the suffixes are applied in order.
     * The counter then drops to 14 (1110) which would mean all but the last suffix are applied.... and so on until zero where no suffixes are applied
     *
     * if propertyName is "BASE" and the suffixes are ONE, TWO, THREE and FOUR then the output is (with counter for reference)
     *
     * (1111) BASE.ONE.TWO.THREE.FOUR
     * (1110) BASE.ONE.TWO.THREE
     * (1101) BASE.ONE.TWO.FOUR
     * (1100) BASE.ONE.TWO
     * (1011) BASE.ONE.THREE.FOUR
     * (1010) BASE.ONE.THREE
     * (1001) BASE.ONE.FOUR
     * (1000) BASE.ONE
     * (0111) BASE.TWO.THREE.FOUR
     * (0110) BASE.TWO.THREE
     * (0101) BASE.TWO.FOUR
     * (0100) BASE.TWO
     * (0011) BASE.THREE.FOUR
     * (0010) BASE.THREE
     * (0001) BASE.FOUR
     * (0000) BASE
     *
     * @return the list of property names
     */
    public static List<String> generatePropertyNameList(String propertyName, List<String> suffixValues) {
        List<String> propertyNames = new ArrayList<>();
        int suffixes = suffixValues.size();
        if (suffixes > 0) {
            //starting values for the counter based on the number of suffixes
            // 4        1111            15
            // 3        0111            7
            // 2        0011            3
            // 1        0001            1

            //bit masks for the suffixes
            //1 << 3        1000
            //1 << 2        0100
            //1 << 1        0010
            //1 << 0        0001
            int counter = ((int) Math.pow(2, suffixes)) - 1;
            while (counter > 0) {
                StringBuilder builder = new StringBuilder(propertyName);
                for (int i = 0; i < suffixes; i++) {
                    int shift = suffixes - (i + 1);
                    int mask = (1 << shift);
                    if ((counter & mask) == mask) {
                        builder.append(".");
                        builder.append(suffixValues.get(i));
                    }
                }
                String generatedPropertyName = builder.toString();
                propertyNames.add(generatedPropertyName);
                counter--;
            }
        }
        propertyNames.add(propertyName);
        return propertyNames;
    }

}
