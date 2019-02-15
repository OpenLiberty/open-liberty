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
package com.ibm.ws.microprofile.config14.interfaces;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public interface WebSphereConfig14 extends WebSphereConfig, Consumer<Set<String>> {

    public SourcedValue getSourcedValue(String key, Type type, boolean evaluateVariables);

    /**
     * @param key
     * @param type
     * @param defaultString
     * @param evaluateVariables
     * @param converter
     * @return
     */
    public SourcedValue getSourcedValue(List<String> keys, Type type, Class<?> genericSubType, Object defaultValue, boolean evaluateVariables,
                                        Converter<?> converter);

    /**
     * Get the converted value of the given property.
     * If the property is not found and optional is true then use the default string to create a value to return.
     * If the property is not found and optional is false then throw an exception.
     *
     * @param propertyName      the property to get
     * @param propertyType      the type to convert to
     * @param optional          is the property optional
     * @param defaultString     the default string to use if the property was not found and optional is true
     * @param evaluateVariables if true, resolve variables within the value
     * @return the converted value
     * @throws NoSuchElementException thrown if the property was not found and optional was false
     */
    public Object getValue(String propertyName, Type propertyType, boolean optional, String defaultString, boolean evaluateVariables);

    public void registerPropertyChangeListener(PropertyChangeListener listener, String propertyName);

    @FunctionalInterface
    public static interface PropertyChangeListener {
        public void onPropertyChanged();
    }
}
