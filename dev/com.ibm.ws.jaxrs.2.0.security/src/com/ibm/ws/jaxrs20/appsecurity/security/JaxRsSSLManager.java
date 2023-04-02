/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.appsecurity.security;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;

public class JaxRsSSLManager {
    private static final TraceComponent tc = Tr.register(JaxRsSSLManager.class);

    private static final Map<String, SSLSocketFactory> socketFactories = new HashMap<>();
    private static final Map<String, SSLContext> sslContexts = new HashMap<>();
    private static final JSSEHelper jsseHelper = JSSEHelper.getInstance();

    /**
     * Get the SSLSocketFactory by sslRef, if could not get the configuration, try use the server's default
     * ssl configuration when fallbackOnDefault = true
     *
     * @param sslRef
     * @param host   - used to get the SSLSocketFactory from JSSEHelper
     * @param port   - used to get the SSLSocketFactory from JSSEHelper
     * @return
     */
    public static SSLSocketFactory getSSLSocketFactoryBySSLRef(String sslRef, String host, String port) {
        SSLSocketFactory sslSocketFactory = null;

        try {
            Map<String, Object> connectionInfo = getConnectionInfo(host, port);
            Properties sslProps = getSSLProperties(sslRef, connectionInfo);
            SSLContext sslContext = getSSLContext(sslRef, connectionInfo, sslProps);

            if (sslContext == null) {
                return null;
            }

            boolean recache = false;
            synchronized (sslContexts) {
                SSLContext cachedSslContext = sslContexts.get(sslRef);
                if (sslContext == null || !sslContext.equals(cachedSslContext)) {
                    // first request or SSL config has changed, re-cache the SSLContext and SSLSocketFactory
                    sslContexts.put(sslRef, sslContext);
                    recache = true;
                }
            }

            synchronized (socketFactories) {
                sslSocketFactory = socketFactories.get(sslRef);
                if (sslSocketFactory == null || recache) {
                    sslSocketFactory = sslContext.getSocketFactory();
                    socketFactories.put(sslRef, sslSocketFactory);
                }
            }
        } catch (com.ibm.websphere.ssl.SSLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "configClientSSL failed to get the SSLSocketFactory with exception: " + e.toString());
            }
            return null;
        }
        return sslSocketFactory;
    }

    /**
     * Get the SSL cipher list by sslRef
     *
     * @param sslRef
     * @param host   - used to get the SSLContext from JSSEHelper
     * @param port   - used to get the SSLContext from JSSEHelper
     * @return
     */
    public static String[] getSSLCipherSuitesBySSLRef(String sslRef, String host, String port) {
        String ciphers[] = null;

        try {
            Map<String, Object> connectionInfo = getConnectionInfo(host, port);
            Properties sslProps = getSSLProperties(sslRef, connectionInfo);
            SSLContext sslContext = getSSLContext(sslRef, connectionInfo, sslProps);

            if (sslContext == null) {
                return null;
            }

            String cipherString = sslProps.getProperty(Constants.SSLPROP_ENABLED_CIPHERS);

            try {

                if (cipherString != null) {
                    ciphers = cipherString.split("\\s+");
                } else {
                    String securityLevel = sslProps.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "securityLevel from properties is " + securityLevel);
                    if (securityLevel == null)
                        securityLevel = "HIGH";

                    ciphers = Constants.adjustSupportedCiphersToSecurityLevel(sslContext.createSSLEngine().getEnabledCipherSuites(), securityLevel);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception getting ciphers.", new Object[] { e });
            }
        } catch (com.ibm.websphere.ssl.SSLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "configClientSSL failed to get the supported SSL ciphers with exception: " + e.toString());
            }
            return null;
        }

        return ciphers;
    }

    /**
     * Get the SSL protocol list by sslRef
     *
     * @param sslRef
     * @param host   - used to get the SSLContext from JSSEHelper
     * @param port   - used to get the SSLContext from JSSEHelper
     * @return
     */
    public static String[] getSSLProtocolsBySSLRef(String sslRef, String host, String port) {
        String[] protocolList = null;

        try {
            ArrayList<String> list = new ArrayList<String>();
            Map<String, Object> connectionInfo = getConnectionInfo(host, port);
            Properties sslProps = getSSLProperties(sslRef, connectionInfo);

            String cfgProtocolValue = sslProps.getProperty(Constants.SSLPROP_PROTOCOL);
            if (cfgProtocolValue != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "protocols from config are " + cfgProtocolValue);
                String[] protocols = cfgProtocolValue.split(",");

                // Only setting value if a list is provided, need to check the list for good values
                for (String protocol : protocols) {
                    if (Constants.MULTI_PROTOCOL_LIST.contains(protocol))
                        list.add(protocol);
                }
                if (!list.isEmpty())
                    protocolList = list.toArray(new String[list.size()]);
            }
        } catch (com.ibm.websphere.ssl.SSLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "configClientSSL failed to get the SSL protocol with exception: " + e.toString());
            }
            return null;
        }

        return protocolList;
    }

    private static Map<String, Object> getConnectionInfo(String host, String port) {
        Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port); // String expected by OutboundSSLSelections
        return connectionInfo;
    }

    private static Properties getSSLProperties(String sslRef, Map<String, Object> connectionInfo) throws SSLException {
        Properties sslProps;
        try {
            sslProps = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return jsseHelper.getProperties(sslRef, connectionInfo, null);
                }
            });

        } catch (PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            throw (SSLException) cause;
        }

        return sslProps;
    }

    private static SSLContext getSSLContext(String sslRef, Map<String, Object> connectionInfo, Properties sslProps) throws SSLException {
        Boolean sslCfgExists = null;
        if (sslRef != null) {
            try {
                sslCfgExists = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                    @Override
                    public Boolean run() throws SSLException {
                        return Boolean.valueOf(jsseHelper.doesSSLConfigExist(sslRef));
                    }
                });

            } catch (PrivilegedActionException pae) {
                Throwable cause = pae.getCause();
                throw (SSLException) cause;
            }

            if (!sslCfgExists.booleanValue())
                return null;
        }

        return jsseHelper.getSSLContext(connectionInfo, sslProps);
    }
}
