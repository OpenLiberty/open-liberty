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
package com.ibm.ws.jaxws.clientcontainer.security;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Using JaxWsSSLManager to do the SSL stuff
 */
public class JaxWsSSLManager {
    private static final TraceComponent tc = Tr.register(JaxWsSSLManager.class);
    private static final AtomicReference<AtomicServiceReference<SSLSupport>> sslSupportServiceRef = new AtomicReference<AtomicServiceReference<SSLSupport>>();

    protected static void init(AtomicServiceReference<SSLSupport> sslSupportSR) {
        sslSupportServiceRef.set(sslSupportSR);
    }

    public static SSLSocketFactory getProxySSLSocketFactoryBySSLRef(String sslRef, Map<String, Object> props) {
        return new JaxWsProxySSLSocketFactory(sslRef, props);
    }

    public static SSLSocketFactory getProxyDefaultSSLSocketFactory(Map<String, Object> props) {
        return new JaxWsProxySSLSocketFactory(JaxWsSecurityConstants.SERVER_DEFAULT_SSL_CONFIG_ALIAS, props);
    }

    /**
     * Get the SSLSocketFactory by sslRef, if could not get the configuration, try use the server's default
     * ssl configuration when fallbackOnDefault = true
     * 
     * @param sslRef
     * @param props the additional props to override the properties in SSLConfig
     * @param fallbackOnDefault if true, will fall back on server default ssl configuration
     * @return
     */
    public static SSLSocketFactory getSSLSocketFactoryBySSLRef(String sslRef, Map<String, Object> props, boolean fallbackOnDefault) {
        SSLSupport sslSupportService = tryGetSSLSupport();

        if (null == sslSupportService) {
            return null;
        }

        JSSEHelper jsseHelper = sslSupportService.getJSSEHelper();
        Properties sslConfig = null;
        SSLConfig sslConfigCopy = null;
        try {
            sslConfig = jsseHelper.getProperties(sslRef);
            if (null != sslConfig) {
                // must copy one
                sslConfigCopy = new SSLConfig(sslConfig);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot get the ssl configuration by sslRef=" + sslRef);
                }

                if (fallbackOnDefault) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Try to get the default ssl configuration of server");
                    }
                    // maybe use something like: <sslDefault sslRef="myDefaultSSLConfig">
                    sslConfig = jsseHelper.getProperties(null, null, null);
                    if (null != sslConfig) {
                        sslConfigCopy = new SSLConfig(sslConfig);
                    }
                }
            }

            if (null == sslConfigCopy) {
                return null;
            }
            // override the existed property in SSLConfig
            if (null != props && !props.isEmpty()) {
                Iterator<Map.Entry<String, Object>> iter = props.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, Object> entry = iter.next();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, entry.getKey() + "=" + entry.getValue() + " is overriden in SSLConfig=" + sslRef);
                    }
                    sslConfigCopy.put(entry.getKey(), entry.getValue());
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Get the SSLSocketFactory by sslRef=" + sslRef);
            }

            return sslSupportService.getJSSEProvider().getSSLSocketFactory(null, sslConfigCopy);
        } catch (SSLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "err.when.get.ssl.config", sslRef);
            }
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "err.when.get.ssl.socket.factory", sslRef);
            }
            throw new IllegalStateException(e);
        }
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
