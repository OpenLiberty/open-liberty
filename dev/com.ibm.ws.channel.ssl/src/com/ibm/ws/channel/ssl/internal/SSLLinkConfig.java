/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.util.Properties;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;

/**
 * Configuration used on the individual SSL connections. This may or may not
 * match the owning channel configuration as the configuration can come from
 * that, from a repertoire, or even from on-thread programmatic properties.
 */
public class SSLLinkConfig {

    /** Trace component for WAS */
    private static final TraceComponent tc = Tr.register(SSLLinkConfig.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    /** Configuration reference */
    private Properties myConfig = null;

    /**
     * Constructor.
     *
     * @param config
     */
    public SSLLinkConfig(Properties config) {
        this.myConfig = config;
    }

    /**
     * Access a boolean property.
     *
     * @param key
     * @return boolean - false if the property does not exist
     */
    public boolean getBooleanProperty(String key) {
        return "true".equalsIgnoreCase(this.myConfig.getProperty(key));
    }

    /**
     * Access a property.
     *
     * @param key
     * @return String
     */
    public String getProperty(String key) {
        return this.myConfig.getProperty(key);
    }

    /**
     * Access the set of properties.
     *
     * @return Properties
     */
    public Properties getProperties() {
        return this.myConfig;
    }

    /**
     * Query the list of enabled cipher suites for this connection.
     *
     * @param sslEngine
     * @return String[]
     */
    public String[] getEnabledCipherSuites(SSLEngine sslEngine) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getEnabledCipherSuites");
        }
        String ciphers[] = null;

        // First check the properties object for the ciphers.
        Object ciphersObject = this.myConfig.get(Constants.SSLPROP_ENABLED_CIPHERS);
        if (null == ciphersObject) {
            // Did not find the enabled ciphers. Need to determine them here.
            String securityLevel = this.myConfig.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
            if (null == securityLevel) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Defaulting to HIGH security level");
                }
                securityLevel = Constants.SECURITY_LEVEL_HIGH;
            }
            // Found the security level.
            ciphers = Constants.adjustSupportedCiphersToSecurityLevel(
                                                                      sslEngine.getSupportedCipherSuites(), securityLevel);
        } else {
            // Found enabled cipher suites. Now we need to put them in the right kind of object.
            if (ciphersObject instanceof String) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "enabledCipherSuites is a String: " + ciphersObject);
                }
                // Quickly break the string up into an array based on space delimiters.
                ciphers = ((String) ciphersObject).split("\\s");
            } else if (ciphersObject instanceof String[]) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "enabledCipherSuites is a String array");
                }
                ciphers = (String[]) ciphersObject;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Invalid object for enabledCipherSuites: " + ciphersObject);
                }
            }
        }
        // check for when we're returning 0 ciphers as the connection will not
        // work and will be throwing errors later on
        if (null == ciphers || 0 == ciphers.length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to find any enabled ciphers");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getEnabledCipherSuites");
        }
        return ciphers;
    }

    /**
     * Query the SSL Protocol for this connection.
     *
     * @return String
     */
    public String getSSLProtocol() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSSLProtocol");
        }

        // First check the properties object for the ciphers.
        String protocol = (String) this.myConfig.get(Constants.SSLPROP_PROTOCOL);

        // we only want to set the protocol on the engine if it a specific protocol name
        // don't set to TLS or SSL
        if (protocol.equals(Constants.PROTOCOL_SSL) || protocol.equals(Constants.PROTOCOL_TLS))
            protocol = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSSLProtocol " + protocol);
        }
        return protocol;
    }

}
