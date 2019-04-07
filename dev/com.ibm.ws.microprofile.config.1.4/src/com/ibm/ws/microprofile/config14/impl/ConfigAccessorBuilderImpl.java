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

import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;

public class ConfigAccessorBuilderImpl<T> extends AbstractConfigAccessorBuilder<T> implements ConfigAccessorBuilder<T> {

    private final Class<T> conversionType;

    public ConfigAccessorBuilderImpl(WebSphereConfig14 config, String propertyName, Class<T> conversionType) {
        super(config, propertyName);
        this.conversionType = conversionType;
    }

//    @Override
//    public ConfigAccessorBuilder<List<T>> asList() {
//        return new ConfigAccessorBuilderListImpl<T>(this);
//    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<T> build() {
        ConfigAccessor<T> accessor;
        synchronized (this) {
            List<String> propertyNames = generatePropertyNameList(getPropertyName(), getSuffixValues());
            Duration cacheForDuration = getCacheFor();
            boolean evaluateVariables = getEvaluateVariables();
            Object defaultValue = getDefaultValue();
            String defaultString = getDefaultString();
            Converter<T> converter = getConverter();
            Class<T> conversionType = getConversionType();

            accessor = new ConfigAccessorImpl<T>(getConfig(), propertyNames, conversionType, null, cacheForDuration, evaluateVariables, defaultValue, defaultString, converter);
        }
        return accessor;
    }

    /**
     * @return
     */
    protected Class<T> getConversionType() {
        return this.conversionType;
    }

}
