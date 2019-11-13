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
package com.ibm.ws.jaxws.security.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
        try {
            // get the properties from the jsseHelper
            if (sslRef != null) {
                sslConfig = getSSLConfig(sslRef, jsseHelper);
            }

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

            if (sslConfig != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Get the SSLSocketFactory by properties =" + sslConfig);
                }
                return sslSupportService.getSSLSocketFactory(sslConfig);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Get the default SSLSocketFactory");
                }
                return sslSupportService.getSSLSocketFactory();
            }
        } catch (SSLException e) {
            Tr.error(tc, "err.when.get.ssl.config", sslRef);
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            Tr.error(tc, "err.when.get.ssl.socket.factory", sslRef, e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private static Properties getSSLConfig(String sslRef, JSSEHelper jsseHelper) throws SSLException {
        final String f_sslRef = sslRef;
        final JSSEHelper f_jsseHelper = jsseHelper;
        Properties sslConfig = null;
        try {
            sslConfig = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return f_jsseHelper.getProperties(f_sslRef);
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
