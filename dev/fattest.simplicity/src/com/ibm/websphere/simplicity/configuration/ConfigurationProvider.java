/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * The <code>ConfigurationProvider</code> provides methods to read and set properties within
 * configuration files. A configuration file in this context is an abstraction for any file used to
 * feed user defined data to the project such as the bootstrapping properties file. When a
 * configuration file is loaded, the properties are loaded into memory and any
 * {@link #getProperty(String)}, {@link #setProperty(String, String)}, and other related method
 * calls are read and loaded into memory. Any property value changes are not reflected in the
 * configuration file itself until a call to {@link #writeProperties()} is made.
 */
public abstract class ConfigurationProvider {

    protected static Class c = ConfigurationProvider.class;

    private final File configFile;

    /**
     * Constructor
     *
     * @param configFile The configuration file to interact with
     */
    protected ConfigurationProvider(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Check if a property exists in memory
     *
     * @param  property The property name
     * @return          true if the property exists
     */
    public abstract boolean hasProperty(String property);

    /**
     * Get a property value from memory
     *
     * @param  property The name of the property to get
     * @return          The property value
     */
    public abstract String getProperty(String property);

    /**
     * Retrieves all properties prefixed by a particular key.
     *
     * @param  key The prefix for the properties to get
     * @return     All properties prefixed by the key
     */
    public abstract Properties getProperties(String key);

    /**
     * Set a property in memory
     *
     * @param property The name of the property to set
     * @param value    The value of the property to set
     */
    public abstract void setProperty(String property, String value);

    /**
     * Set multiple properties in memory
     *
     * @param props A <code>Map</code> containing the property names and values to set
     */
    public abstract void setProperties(Map<String, String> props);

    /**
     * Remove a property in memory
     *
     * @param property The name of the property to remove
     */
    public abstract void removeProperty(String property);

    /**
     * Get an <code>Enumeration</code> instance containing the names of the available properties
     *
     * @return An <code>Enumeration</code> instance containg the property names
     */
    public abstract Enumeration<?> getPropertyNames();

    /**
     * Write the property names and values currently in memory to the configuration file.
     *
     * @throws Exception
     */
    public abstract void writeProperties() throws Exception;

    /**
     * This method clears all property values in memory and reloads the configuration file.
     *
     * @throws Exception
     */
    public abstract void reloadProperties() throws Exception;

    /**
     * Get the configuration file
     *
     * @return A <code>File</code> representing the configuration file for this instance
     */
    public File getConfigFile() {
        return this.configFile;
    }

    /**
     * Get a <code>FileInputStream</code> for direct read access to the configuration file. Note
     * that the properties in the file may not reflect uncommitted changes.
     *
     * @return           A <code>FileInputStream</code> instance
     * @throws Exception
     */
    public InputStream getInputStream() throws Exception {
        return new FileInputStream(this.configFile);
    }

    /**
     * Get a <code>FileOutputStream</code> for write read access to the configuration file. Note
     * that the properties in the file may not reflect uncommitted changes.
     *
     * @param  append    True if writes should be appended to the existing file
     * @return           A <code>FileOutputStream</code> instance
     * @throws Exception
     */
    public OutputStream getOutputStream(boolean append) throws Exception {
        return new FileOutputStream(this.configFile, append);
    }

}
