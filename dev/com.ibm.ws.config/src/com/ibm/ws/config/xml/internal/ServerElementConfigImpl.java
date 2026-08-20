/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.config.xml.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.kernel.server.ServerElementConfig;

/**
 * Implementation of ServerElementConfig service.
 * This service provides access to configuration values that are defined
 * as attributes on the <server> element in server.xml.
 *
 * This is a thin facade that delegates to ServerXMLConfiguration/BaseConfiguration
 * where the actual values are stored.
 */
@Component(service = ServerElementConfig.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class ServerElementConfigImpl implements ServerElementConfig {

    
    /**
     * Singleton instance for direct access within the same bundle.
     */
    private static volatile ServerElementConfigImpl instance;
    
    /**
     * Static reference to ServerXMLConfiguration set by SystemConfiguration.
     * This allows access to the actual configuration values stored in BaseConfiguration.
     * Set once during initialization and never modified.
     */
    private static ServerXMLConfiguration serverXMLConfiguration;
    
    /**
     * DS activation method - sets the singleton instance.
     */
    protected void activate() {
        instance = this;
    }
    
    /**
     * DS deactivation method - clears the singleton instance.
     */
    protected void deactivate() {
        instance = null;
    }
    
    /**
     * Set the ServerXMLConfiguration reference. Called by SystemConfiguration.
     *
     * @param config the ServerXMLConfiguration instance
     */
    public static void setServerXMLConfiguration(ServerXMLConfiguration config) {
        serverXMLConfiguration = config;
    }
    
    /**
     * Get the singleton instance for direct access within the same bundle.
     *
     * @return the singleton instance, or null if not yet activated
     */
    public static ServerElementConfigImpl getInstance() {
        return instance;
    }
    
    /**
     * Get the server description from BaseConfiguration.
     *
     * @return the server description, or null if not set or configuration not available
     */
    @Override
    public String getDescription() {
        ServerXMLConfiguration config = serverXMLConfiguration;
        if (config != null) {
            ServerConfiguration serverConfig = config.getConfiguration();
            if (serverConfig != null) {
                return serverConfig.getDescription();
            }
        }
        return null;
    }
    
    /**
     * Get the current quiesce timeout value in milliseconds from BaseConfiguration.
     *
     * @return the quiesce timeout in milliseconds, or default if configuration not available
     */
    @Override
    public long getQuiesceTimeoutMillis() {
        ServerXMLConfiguration config = serverXMLConfiguration;
        if (config != null) {
            ServerConfiguration serverConfig = config.getConfiguration();
            if (serverConfig != null) {
                return serverConfig.getQuiesceTimeoutMillis();
            }
        }
        return BaseConfiguration.DEFAULT_QUIESCE_TIMEOUT_MILLIS;
    }
    
    /**
     * Check if the quiesce timeout was explicitly configured in server.xml.
     *
     * @return true if the user specified a quiesceTimeout attribute, false if using the default
     */
    @Override
    public boolean isQuiesceTimeoutExplicitlyConfigured() {
        ServerXMLConfiguration config = serverXMLConfiguration;
        if (config != null) {
            ServerConfiguration serverConfig = config.getConfiguration();
            if (serverConfig != null) {
                return serverConfig.isQuiesceTimeoutExplicitlyConfigured();
            }
        }
        return false;
    }
}
