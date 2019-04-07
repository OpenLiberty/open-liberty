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
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 */
public class ConfigAccessorBuilderListImpl<T> extends AbstractConfigAccessorBuilder<List<T>> implements ConfigAccessorBuilder<List<T>> {

    private final Class<T> genericSubType;

    /**
     * @param config
     * @param propertyName
     * @param conversionType
     */
    @SuppressWarnings("unchecked")
    public ConfigAccessorBuilderListImpl(ConfigAccessorBuilderImpl<T> source) {
        super(source.getConfig(), source.getPropertyName());
        this.genericSubType = source.getConversionType();
        cacheFor(source.getCacheFor());
        evaluateVariables(source.getEvaluateVariables());

        Object defaultValue = source.getDefaultValue();
        if (!ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
            withDefault(Collections.singletonList((T) defaultValue));
        }

//        withStringDefault(source.getDefaultString());
//        addLookupSuffixes(source.getSuffixValues());

    }

//    /** {@inheritDoc} */
//    @Override
//    public ConfigAccessorBuilder<List<List<T>>> asList() {
//        //this is already a list so asList does not make sense
//        throw new UnsupportedOperationException();
//    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public ConfigAccessor<List<T>> build() {
        ConfigAccessor<List<T>> accessor;
        synchronized (this) {
            List<String> propertyNames = generatePropertyNameList(getPropertyName(), getSuffixValues());
            Duration cacheForDuration = getCacheFor();
            boolean evaluateVariables = getEvaluateVariables();
            Object defaultValue = getDefaultValue();
            String defaultString = getDefaultString();
            Class<T> genericSubType = getGenericSubType();

            //notice that these next couple of lines do not have generics on ... it does work but is not pretty
            @SuppressWarnings("rawtypes")
            Converter converter = getConverter();
            @SuppressWarnings("rawtypes")
            ConfigAccessor rawAccessor = new ConfigAccessorImpl<List>(getConfig(), propertyNames, List.class, genericSubType, cacheForDuration, evaluateVariables, defaultValue, defaultString, converter);
            //and this is an unchecked conversion
            accessor = rawAccessor;
        }
        return accessor;
    }

    /**
     * @return the generic sub type of the List
     */
    protected Class<T> getGenericSubType() {
        return this.genericSubType;
    }

}
