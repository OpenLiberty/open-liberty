/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.interfaces;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;

/**
 * Additional internal Config methods
 */
public interface WebSphereConfig extends Config, Closeable {

    /**
     * The same as {@link #getValue(String, Class)} but uses a Type argument instead of a Class.
     * This allows for generics such as List<MyObject>
     *
     * @param propertyName The name of the property
     * @param type The type to convert to
     * @return The converted value
     * @throws NoSuchElementException thrown if the requested property does not exist
     * @throws IllegalArgumentException thrown if the raw String property value was not compatible with the converter for the specified type
     */
    public Object getValue(String propertyName, Type type);

    /**
     * The same as {@link #getValue(String, Type)} except that if optional is true then ConfigProperty.UNCONFIGURED_VALUE is passed to the converter
     * rather than throwing a NoSuchElementException.
     *
     * @param propertyName The name of the property
     * @param type The type to convert to
     * @return The converted value
     * @throws NoSuchElementException thrown if optional is false and the requested property does not exist
     * @throws IllegalArgumentException thrown if the raw String property value was not compatible with the converter for the specified type
     */
    public Object getValue(String propertyName, Type type, boolean optional);

    /**
     * The same as {@link #getValue(String, Type)} except that the defaultString is passed to the converter
     * rather than throwing a NoSuchElementException.
     *
     * @param propertyName The name of the property
     * @param type The type to convert to
     * @param defaultString the default string to pass to the converter if the requested property does not exist
     * @return The converted value
     * @throws IllegalArgumentException thrown if the raw String property value was not compatible with the converter for the specified type
     */
    public Object getValue(String propertyName, Type type, String defaultString);

    /**
     * Convert the given rawValue into the specified type
     *
     * @param rawValue The raw String value to convert
     * @param type The type to convert to
     * @return The converted value
     * @throws IllegalArgumentException thrown if the raw String value was not compatible with the converter for the specified type
     */
    public Object convertValue(String rawValue, Type type);

    /**
     * Convert the given rawValue into the specified type
     * 
     * @param <T>
     *
     * @param rawValue The raw String value to convert
     * @param type The type to convert to
     * @return The converted value
     * @throws IllegalArgumentException thrown if the raw String value was not compatible with the converter for the specified type
     */
    public <T> T convertValue(String rawValue, Class<T> type);

    /**
     * The same as {@link #getValue(String, Type)} except that the value is wrapped in a SourcedPropertyValue which can also provide the id
     * if the ConfigSource that the property came from.
     *
     * @param propertyName The name of the property
     * @param type The type to convert to
     * @return A SourcedPropertyValue containing the converted value and the source id
     * @throws NoSuchElementException thrown if the requested property does not exist
     * @throws IllegalArgumentException thrown if the raw String property value was not compatible with the converter for the specified type
     */
    public SourcedValue getSourcedValue(String propertyName, Type type);

    /**
     * Generate a human readable String representation of all of the properties in the Config, giving their property names and which source they came from.
     *
     * @return a String representation of the config
     */
    public String dump();
}
