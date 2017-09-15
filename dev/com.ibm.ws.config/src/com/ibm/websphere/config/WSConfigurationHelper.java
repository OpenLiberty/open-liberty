/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.config;

import java.io.InputStream;
import java.util.Dictionary;

/**
 *
 */
public interface WSConfigurationHelper {

    /**
     * Retrieve a dictionary containing the default metatype properties
     *
     * @param pid The full pid or factoryPid value for the configuration
     * @return A Dictionary containing the default properties
     * @throws ConfigEvaluatorException
     */
    Dictionary<String, Object> getMetaTypeDefaultProperties(String pid) throws ConfigEvaluatorException;

    /**
     * Add a default configuration instance. If this affects existing configurations they
     * will be updated.
     *
     * @param pid The full pid or factoryPid value for the configuration
     * @param properties A Dictionary that contains the configuration properties
     * @throws ConfigUpdateException
     */
    void addDefaultConfiguration(String pid, Dictionary<String, String> properties) throws ConfigUpdateException;

    /**
     * Add a default configuration instance using an InputStream that points to valid XML.
     * If this affects existing configurations they will be updated.
     *
     * @param defaultConfig An InputStream that points to valid XML
     * @throws ConfigUpdateException
     */
    void addDefaultConfiguration(InputStream defaultConfig) throws ConfigUpdateException;

    /**
     * Remove all default configurations with the specified pid or factoryPid. This only affects
     * configurations that were added by this interface. If the removal of the default
     * configuration affects existing configurations, they will be updated.
     *
     * @param pid The pid or factoryPid
     * @return true if any configurations were removed
     * @throws ConfigUpdateException
     */
    boolean removeDefaultConfiguration(String pid) throws ConfigUpdateException;

    /**
     * Remove default configurations with the specified pid or factoryPid and ID value. This only
     * affects configurations that were added by this interface. If the removal of the default
     * configuration affects existing configurations, they will be updated.
     *
     * @param pid The pid or factoryPid
     * @param id The id property of the default configuration
     * @return true if any configurations were removed
     * @throws ConfigUpdateException
     */
    boolean removeDefaultConfiguration(String pid, String id) throws ConfigUpdateException;

    /**
     * Returns the metatype attribute name for the configuration with the specified pid and attribute id.
     *
     * @param pid The pid or factory pid
     * @param attributeID
     * @return the name for the specified attribute or null if the attribute or configuration element don't exist.
     */
    String getMetaTypeAttributeName(String pid, String attributeID);

    /**
     * Returns the metatype element name for the configuration with the specified pid.
     *
     * @param pid The pid or factory pid
     * @return the name for the specified element or null if the configuration element don't exist.
     */
    String getMetaTypeElementName(String pid);

    /**
     * Returns whether a registry entry exists for the supplied pid.
     *
     * @param pid The pid or factory pid
     * @return true if the registry entry exists for the supplied pid
     */
    boolean registryEntryExists(String pid);

    /**
     * @param pid The pid to find the alias for
     * @param baseAlias If the pid parameter declares an extended alias via ibm:extendsAlias
     *            (instead of ibm:alias), the baseAlias will be prepended to the returned alias.
     *            May be null.
     * @return the alias for a given pid
     */
    String aliasFor(String pid, String baseAlias);
}
