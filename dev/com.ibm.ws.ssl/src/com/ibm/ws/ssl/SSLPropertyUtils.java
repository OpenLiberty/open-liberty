/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ssl;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.ws.ssl.config.ProtocolHelper;
import com.ibm.ws.ssl.config.SSLConfigManager;

/**
 * A utility class for {@link LibertySSLSocketFactoryWrapper} and {@link LibertySSLServerSocketFactoryWrapper}
 * to get Liberty's SSL Properties and configure them on their respective {@code Sockets}
 */
@SuppressWarnings("removal")
public class SSLPropertyUtils {

    private static final TraceComponent tc = Tr.register(SSLPropertyUtils.class, "SSL", "com.ibm.ws.ssl");

    /**
     * Convenience method to call {@link JSSEHelper#getProperties(String, Map, SSLConfigChangeListener))} with elevated privileges.
     *
     * @param sslAliasName          The alias name of the SSL configuration.
     * @param currentConnectionInfo Remote connection information.
     * @param listener              Listener for SSL configuration updates.
     * @return The properties.
     * @throws com.ibm.websphere.ssl.SSLException
     * @see JSSEHelper#getProperties(String, Map, SSLConfigChangeListener)
     */
    @SuppressWarnings({ "deprecation" })
    private static java.util.Properties getProperties(final String sslAliasName, final Map<String, Object> currentConnectionInfo,
                                                      final SSLConfigChangeListener listener) throws com.ibm.websphere.ssl.SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getProperties sslAliasName=" + sslAliasName
                         + "\ncurrentConnectionInfo=" + currentConnectionInfo
                         + "\nlistener=" + listener);

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<java.util.Properties>() {
                @Override
                public java.util.Properties run() throws Exception {
                    return JSSEHelper.getInstance().getProperties(sslAliasName, currentConnectionInfo, listener);
                }
            });
        } catch (PrivilegedActionException e) {
            /*
             * Can only be SSLException or RuntimeException.
             */
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw (com.ibm.websphere.ssl.SSLException) cause;
            }
        }
    }

    protected static Properties lookupProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "lookupProperties");

        return lookupProperties(null);
    }

    /*
     * Lookup the SSL properties from config. If no alias is provided, then use the default ssl config.
     */
    protected static Properties lookupProperties(String alias) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "lookupProperties alias=" + alias);

        Properties properties = null;
        if (alias != null) { // get configuration for the supplied alias
            try {
                java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
                connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
                properties = getProperties(alias, connectionInfo, null);
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Failed getting SSL config from alias=" + alias, new Object[] { e });
                // TODO: Add a translated error message
            }
        } else { // no alias provided, get the default SSL configuration
            properties = getDefaultProperties();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "lookupProperties properties=" + properties);

        return properties;
    }

    private static Properties getDefaultProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getDefaultProperties");

        Properties properties = null;
        try {
            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);

            properties = SSLConfigManager.getInstance().getDefaultSystemProperties(true);
            if (properties == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Getting default SSL properties from WebSphere configuration.");
                properties = getProperties(null, connectionInfo, null);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Getting javax.net.ssl.* SSL System properties.");
            }

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LibertySSLSocketFactory exception getting default SSL properties.", new Object[] { e });
            properties = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getDefaultProperties properties=" + properties);

        return properties;
    }

    protected static void setSSLPropertiesOnSocket(Properties properties, SSLSocket socket) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setSSLPropertiesOnSocket properties=" + properties);

        // get the existing SSLParameters
        SSLParameters sslParameters = socket.getSSLParameters();

        if (properties != null) {
            // get the cipher suites
            String[] ciphers = SSLConfigManager.getInstance().getCipherList(properties, socket);
            sslParameters = createSSLParameters(properties, sslParameters, ciphers);
            socket.setSSLParameters(sslParameters);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setSSLPropertiesOnSocket");
    }

    protected static void setSSLPropertiesOnServerSocket(Properties properties, SSLServerSocket socket) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setSSLPropertiesOnServerSocket properties=" + properties);

        // get the existing SSLParameters
        SSLParameters sslParameters = socket.getSSLParameters();

        if (properties != null) {
            // get the cipher suites
            String[] ciphers = SSLConfigManager.getInstance().getCipherList(properties, socket);
            sslParameters = createSSLParameters(properties, sslParameters, ciphers);
            socket.setSSLParameters(sslParameters);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setSSLPropertiesOnServerSocket");
    }

    /*
     * Returns SSLParameters to set on the Socket.
     * SSLParameters consist of cipher suites, protocol and hostname verification.
     */
    private static SSLParameters createSSLParameters(Properties properties, SSLParameters sslParameters, String[] ciphers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "createSSLParameters properties=" + properties
                         + "\nsslParameters=" + sslParameters + "\nciphers=" + ciphers);

        if (properties != null) {
            //Set ciphers
            sslParameters.setCipherSuites(ciphers);

            //Set protocol
            String protocol = properties.getProperty(Constants.SSLPROP_PROTOCOL);
            String[] protocols = new ProtocolHelper().getSSLProtocol(protocol);
            if (protocols != null)
                sslParameters.setProtocols(protocols);

            //Enable hostname verification
            String enableEndpointId = properties.getProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION, "true");
            if (enableEndpointId != null && enableEndpointId.equalsIgnoreCase("true")) {
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "createSSLParameters sslParameters=" + sslParameters);

        return sslParameters;
    }
}
