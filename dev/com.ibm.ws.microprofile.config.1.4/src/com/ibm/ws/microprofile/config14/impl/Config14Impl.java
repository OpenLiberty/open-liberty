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
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.ws.microprofile.config.impl.ConfigImpl;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSources;

public class Config14Impl extends ConfigImpl {

    /**
     * @param conversionManager
     * @param sources
     * @param executor
     * @param refreshInterval
     */
    public Config14Impl(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        super(conversionManager, sources, executor, refreshInterval);
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
