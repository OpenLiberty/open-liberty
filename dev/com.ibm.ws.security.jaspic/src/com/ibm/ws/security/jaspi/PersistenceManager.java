/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Date     Defect/feature CMVC ID   Description
 * -------- -------------- --------- -----------------------------------------------
 * 03/05/10 640919         leou      CTS AuthConfigFactoryVerifyPersistence testcase fails. Need to store persistent providers.
 */
package com.ibm.ws.security.jaspi;

import java.io.File;
import java.util.Map;

import javax.security.auth.message.config.AuthConfigFactory;

/**
 * A PersistenceManager object stores/loads representations of JASPI AuthConfigProviders into/from persistent storage.
 * <p>An instance of this interface will be used by {@link ProviderRegistry} in the method {@link AuthConfigFactory#registerConfigProvider(String, Map, String, String, String)}.
 * When an AuthConfigProvider is registered with the above method, its definition must be stored persistently such that it will be
 * re-loaded when ProviderRegistry is initialized after a JVM restart.
 * 
 * @author IBM Corp.
 * 
 */
public interface PersistenceManager {

    /**
     * Name of configuration property that specifies the location and filename where persistent providers are stored.
     * In a WAS client process, the property is a System property and in a WAS server process the proprty is a security
     * configuration custom property.
     */
    public static final String JASPI_CONFIG = "com.ibm.websphere.jaspi.configuration"; // Name of property that specifies file name

    /**
     * Register in persistent storage a representation of an AuthConfigProvider with the given attributes.
     * <p>A new persistent registration is created if one does not exist. Otherwise, the existing registration will be updated
     * with the given arguments.
     * 
     * @param className
     * @param properties
     * @param layer
     * @param appContext
     * @param description
     */
    void registerProvider(String className, Map<String, String> properties, String layer, String appContext, String description);

    /**
     * Remove from persistent storage the registration that matches the given attributes if one exists.
     * 
     * @param layer
     * @param appContext
     */
    void removeProvider(String layer, String appContext);

    /**
     * Search persistent storage for a provider registration with the given attributes and return a model representation of the registration if
     * one exists. Return null if a matching registration is not found.
     * <p>The type is intentionally java.lang.Object in order to support different object-model representations.
     * 
     * @param layer
     * @param appContext
     * @return
     */
    Object getJaspiProvider(String layer, String appContext);

    /**
     * Loads from persistent storage and registers all defined JASPI registrations in AuthConfigFactory using method
     * {@link AuthConfigFactory#registerConfigProvider(String, Map, String, String, String)}.
     * 
     * @throws RuntimeException if an error is encountered when loading/storing the provider registrations from/to persistent storage.
     */
    void load();

    /**
     * Sets the value of the AuthConfigFactory attribute.
     * <p>The AuthConfigFactory will be used by method load() to register persistent provider registrations loaded from persistent storage.
     * 
     * @param factory
     */
    void setAuthConfigFactory(AuthConfigFactory factory);

    /**
     * Return the value of the AuthConfigFactory attribute.
     * 
     * @return
     */
    AuthConfigFactory getAuthConfigFactory();

//	/**
//	 * Sets the value of the SecurityConfig attribute.
//	 * <p>SecurityConfig may be used by implementors to retrieve WAS security configuration data.
//	 * 
//	 * @param securityCfg
//	 */
//	void setSecurityConfig(SecurityConfig securityCfg);
//	/**
//	 * Return the value of the SecurityConfig attribute.
//	 * 
//	 * @return
//	 */
//	SecurityConfig getSecurityConfig();
    /**
     * Sets the File where persistent provider representations will be loaded/stored.
     * 
     * @param file
     */
    void setFile(File file);

    /**
     * Return the File where persistent provider representations will be loaded/stored.
     * 
     * @return
     */
    File getFile();
}
