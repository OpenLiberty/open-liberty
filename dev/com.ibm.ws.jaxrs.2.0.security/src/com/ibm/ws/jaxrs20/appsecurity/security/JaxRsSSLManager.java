/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.appsecurity.security;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
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
        JSSEHelper jsseHelper = JSSEHelper.getInstance();

        try {
            Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port); // String expected by OutboundSSLSelections
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

            SSLContext sslContext = jsseHelper.getSSLContext(connectionInfo, sslProps);

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
}
