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
package com.ibm.ws.microprofile.contextpropagation;

/**
 * This interface abstracts away usage of MicroProfile Config so that values can be
 * conditionally read from MicroProfile Config if available. The only data types
 * needed are String[] and Integer.
 */
public interface MPConfigAccessor {
    /**
     * Reads a String[] or Integer property value from MicroProfile Config.
     *
     * @param config instance of org.eclipse.microprofile.config.Config.
     * @param name config property name.
     * @param defaultValue value to use if a config property with the specified name is not found.
     * @return value from MicroProfile Config. Otherwise the default value.
     */
    <T> T get(Object config, String name, T defaultValue);

    /**
     * Resolve instance of org.eclipse.microprofile.config.Config.
     *
     * @return instance of org.eclipse.microprofile.config.Config.
     */
    Object getConfig();
}
