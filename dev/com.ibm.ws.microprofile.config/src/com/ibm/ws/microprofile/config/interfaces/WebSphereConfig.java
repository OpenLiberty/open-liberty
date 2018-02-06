/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import org.eclipse.microprofile.config.Config;

/**
 *
 */
public interface WebSphereConfig extends Config, Closeable {

    public Object getValue(String propertyName, Type type);

    /**
     * Get a converted value
     *
     * @param propertyName The propertyName
     * @param type The type to convert to
     * @param optional if false then throw NoSuchElementException if the property does not exist
     *            if true and the property does not exist, use a null property value
     * @return
     */
    public Object getValue(String propertyName, Type type, boolean optional);

    /**
     * Get a converted value, using the defaultString if the property does not exist
     *
     * @param propertyName The propertyName
     * @param type The type to convert to
     * @param defaultString the string value to convert if the property does not exist
     * @return
     */
    public Object getValue(String propertyName, Type type, String defaultString);

    public Object convertValue(String rawValue, Type type);

    public String dump();

    public SourcedPropertyValue getSourcedValue(String propertyName, Type type);

}
