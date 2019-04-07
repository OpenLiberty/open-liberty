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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config14.interfaces.Config14Constants;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;

public abstract class AbstractConfigAccessorBuilder<T> implements ConfigAccessorBuilder<T> {

    private final WebSphereConfig14 config;
    private final String propertyName;
    private final List<String> suffixValues = new ArrayList<String>();

    //access to these variables must be synchronized to prevent them being changed during a call to build()
    private Duration cacheFor = Duration.ZERO;
    private boolean evaluateVariables = Config14Constants.ACCESSOR_EVALUATE_VARIABLES_DEFAULT;
    private Object defaultValue = ConfigProperty.UNCONFIGURED_VALUE;
    private String defaultString = ConfigProperty.UNCONFIGURED_VALUE;
    private Converter<T> converter = null;

    protected AbstractConfigAccessorBuilder(WebSphereConfig14 config, String propertyName) {
        this.config = config;
        this.propertyName = propertyName;
    }

    @Override
    public ConfigAccessorBuilder<T> useConverter(Converter<T> converter) {
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
            this.defaultString = ConfigProperty.UNCONFIGURED_VALUE;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessorBuilder<T> withStringDefault(String value) {
        synchronized (this) {
            this.defaultString = value;
            this.defaultValue = ConfigProperty.UNCONFIGURED_VALUE;
        }
        return this;
    }

    @Override
    public ConfigAccessorBuilder<T> cacheFor(Duration cacheFor) {
        if (cacheFor == null) {
            //TODO NLS
            throw new IllegalArgumentException("cacheFor may not be null");
        }
        if (cacheFor.isNegative() || cacheFor.isZero()) {
            //TODO NLS
            throw new IllegalArgumentException("cacheFor value must greater than zero");
        }
        synchronized (this) {
            this.cacheFor = cacheFor;
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

//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessorBuilder<T> addLookupSuffix(String suffixValue) {
//        if (suffixValue == null) {
//            //TODO NLS
//            throw new IllegalArgumentException("suffixValue may not be null");
//        }
//        synchronized (this) {
//            this.suffixValues.add(suffixValue);
//        }
//        return this;
//    }
//
//    public ConfigAccessorBuilder<T> addLookupSuffixes(List<String> suffixValues) {
//        if (suffixValues == null) {
//            //TODO NLS
//            throw new IllegalArgumentException("suffixValues may not be null");
//        }
//        synchronized (this) {
//            this.suffixValues.addAll(suffixValues);
//        }
//        return this;
//    }

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

    //these protected methods should only be called within a synchronized(this) block
    protected WebSphereConfig14 getConfig() {
        return this.config;
    }

    protected String getPropertyName() {
        return this.propertyName;
    }

    protected Duration getCacheFor() {
        return this.cacheFor;
    }

    protected List<String> getSuffixValues() {
        return this.suffixValues;
    }

    protected boolean getEvaluateVariables() {
        return this.evaluateVariables;
    }

    protected Object getDefaultValue() {
        return this.defaultValue;
    }

    protected String getDefaultString() {
        return this.defaultString;
    }

    protected Converter<T> getConverter() {
        return this.converter;
    }
}
