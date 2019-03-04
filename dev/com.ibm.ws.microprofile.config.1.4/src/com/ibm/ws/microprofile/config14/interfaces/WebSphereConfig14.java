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

    /**
     * Similar to {@link #getSourcedValue(List<String>, Type, Class<?>, Object, boolean, Converter<?>)} except that
     * - there is no generic type
     * - there is no default value
     * - the standard converters are always used
     *
     * @param propertyName      The property name to use
     * @param type              The type to convert to
     * @param genericSubType    The generic type if any
     * @param defaultString     The default string to use if the property was not found and optional is true
     * @param evaluateVariables If true, resolve variables within the value
     * @return A SourcedPropertyValue containing the converted value and the source id
     * @throws NoSuchElementException   thrown if the requested property does not exist
     * @throws IllegalArgumentException thrown if the raw String property value was not compatible with the converter for the specified type
     */
    public SourcedValue getSourcedValue(String propertyName, Type type, boolean evaluateVariables);

    /**
     * Similar to {@link #getValue(String, Type, boolean String, boolean)} except that
     * - The list of propertyNames is iterated through until a valid one is found
     * - A generic sub type (e.g. the generic type of a List) can also be supplied
     * - the value is wrapped in a SourcedPropertyValue which can also provide the id if the ConfigSource that the property came from.
     *
     * @param propertyNames     The list of property names to iterate through
     * @param type              The type to convert to
     * @param genericSubType    The generic type if any
     * @param defaultString     The default string to use if the property was not found and optional is true
     * @param evaluateVariables If true, resolve variables within the value
     * @param converter         The converter to use for conversion. May be null, in which case the config's usual converters will be used.
     * @return A SourcedPropertyValue containing the converted value and the source id
     * @throws NoSuchElementException   thrown if the requested property does not exist
     * @throws IllegalArgumentException thrown if the raw String property value was not compatible with the converter for the specified type
     */
    public SourcedValue getSourcedValue(List<String> propertyNames, Type type, Class<?> genericSubType, Object defaultValue, boolean evaluateVariables,
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

    /**
     * Register a listener for any potential changes to a specified property. Note that at the moment, what this strictly means is that the
     * value MAY have changed on one of the ConfigSources available. This does not mean that the source which changed was the highest ordinal
     * and therefore, if the value is requested again, it may remain the same.
     *
     * @param listener
     * @param propertyName
     */
    public void registerPropertyChangeListener(PropertyChangeListener listener, String propertyName);

    @FunctionalInterface
    public static interface PropertyChangeListener {
        public void onPropertyChanged();
    }
}
