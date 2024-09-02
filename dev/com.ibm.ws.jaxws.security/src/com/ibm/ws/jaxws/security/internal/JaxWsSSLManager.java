/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.jaxws.security.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Using JaxWsSSLManager to do the SSL stuff
 */
public class JaxWsSSLManager {
    private static final TraceComponent tc = Tr.register(JaxWsSSLManager.class);

    private static final Map<Map<String, Object>, SSLSocketFactory> socketFactories = new HashMap<>();
    private static final Map<Map<String, Object>, SSLContext> sslContexts = new HashMap<>();
    private static final AtomicReference<AtomicServiceReference<SSLSupport>> sslSupportServiceRef = new AtomicReference<AtomicServiceReference<SSLSupport>>();

    protected static void init(AtomicServiceReference<SSLSupport> sslSupportSR) {
        sslSupportServiceRef.set(sslSupportSR);
    }

    public static SSLSocketFactory getSSLSocketFactoryBySSLRef(String sslRef, Map<String, Object> props, String host, int port) {
        SSLSocketFactory sslSocketFactory = null;

        try {
            Map<String, Object> connectionInfo = getConnectionInfo(host, port);
            SSLContext sslContext = getSSLContext(sslRef, props, connectionInfo);

            if (sslContext == null) {
                return null;
            }

            props.put("sslRef", sslRef); // add sslRef to cache key
            props.putAll(connectionInfo); // add connection info to cache key
            boolean recache = false;
            synchronized (sslContexts) {
                SSLContext cachedSslContext = sslContexts.get(props);
                if (sslContext == null || !sslContext.equals(cachedSslContext)) {
                    // first request or SSL config has changed, re-cache the SSLContext and SSLSocketFactory
                    sslContexts.put(props, sslContext);
                    recache = true;
                }
            }

            synchronized (socketFactories) {
                sslSocketFactory = socketFactories.get(props);
                if (sslSocketFactory == null || recache) {
                    sslSocketFactory = sslContext.getSocketFactory();
                    socketFactories.put(props, sslSocketFactory);
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

    private static Map<String, Object> getConnectionInfo(String host, int port) {
        Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port)); // String expected by OutboundSSLSelections
        return connectionInfo;
    }

    private static SSLContext getSSLContext(String sslRef, Map<String, Object> props, Map<String, Object> connectionInfo) throws SSLException {
        SSLSupport sslSupportService = tryGetSSLSupport();

        if (null == sslSupportService) {
            return null;
        }

        JSSEHelper jsseHelper = sslSupportService.getJSSEHelper();
        // get the properties from the jsseHelper
        Properties sslConfig = getSSLConfig(sslRef, connectionInfo, jsseHelper);

        // override the existed property in SSLConfig
        if (null != props && !props.isEmpty() && sslConfig != null) {
            Iterator<Map.Entry<String, Object>> iter = props.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, Object> entry = iter.next();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, entry.getKey() + "=" + entry.getValue() + " is overriden in SSLConfig=" + sslRef);
                }
                sslConfig.put(entry.getKey(), entry.getValue());
            }
        }

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<SSLContext>() {
                @Override
                public SSLContext run() throws SSLException {
                    if (sslConfig != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Get the SSLContext by properties =" + sslConfig);
                        }
                        return jsseHelper.getSSLContext(connectionInfo, sslConfig);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Get the default SSLContext");
                        }
                        return jsseHelper.getSSLContext(sslRef, connectionInfo, null);
                    }
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (SSLException) pae.getCause();
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private static Properties getSSLConfig(String sslRef, Map<String, Object> connectionInfo, JSSEHelper jsseHelper) throws SSLException {
        final String f_sslRef = sslRef;
        final JSSEHelper f_jsseHelper = jsseHelper;
        Properties sslConfig = null;
        try {
            sslConfig = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return f_jsseHelper.getProperties(f_sslRef, connectionInfo, null);
                }
            });

        } catch (PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            throw (SSLException) cause;
        }
        // If the sslConfig is not null clone as it the properties may be added or modified later
        if (sslConfig != null)
            return (Properties) sslConfig.clone();
        return null;
    }

    private static SSLSupport tryGetSSLSupport() {
        AtomicServiceReference<SSLSupport> serviceRef = sslSupportServiceRef.get();
        if (null == serviceRef) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The sslSupportService is not set yet");
            }
            return null;
        }

        return serviceRef.getService();
    }
}
