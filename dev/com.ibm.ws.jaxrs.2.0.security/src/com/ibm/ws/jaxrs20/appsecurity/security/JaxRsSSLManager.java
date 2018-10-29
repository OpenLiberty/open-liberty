/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.appsecurity.component.SSLSupportService;
import com.ibm.wsspi.ssl.SSLSupport;

public class JaxRsSSLManager {
    private static final TraceComponent tc = Tr.register(JaxRsSSLManager.class);

    public static SSLSocketFactory getProxySSLSocketFactoryBySSLRef(String sslRef, Map<String, Object> props) {
        return SSLSupportService.isSSLSupportServiceReady() ? new JaxRsProxySSLSocketFactory(sslRef, props) : null;
    }

    /**
     * Get the SSLSocketFactory by sslRef, if could not get the configuration, try use the server's default
     * ssl configuration when fallbackOnDefault = true
     *
     * @param sslRef
     * @param props             the additional props to override the properties in SSLConfig
     * @param fallbackOnDefault if true, will fall back on server default ssl configuration
     * @return
     */
    @FFDCIgnore(PrivilegedActionException.class)
    public static SSLSocketFactory getSSLSocketFactoryBySSLRef(String sslRef, Map<String, Object> props, boolean fallbackOnDefault) {

        if (!SSLSupportService.isSSLSupportServiceReady()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The SSL support service is not ready and can't create SSLSocketFactory");
            }
            return null;
        }

        SSLSupport sslSupportService = SSLSupportService.getSSLSupport();

        JSSEHelper jsseHelper = sslSupportService.getJSSEHelper();
        Boolean sslCfgExists = null;
        try {
            final JSSEHelper f_jsseHelper = jsseHelper;
            if (sslRef != null) {
                final String f_sslRef = sslRef;
                try {
                    sslCfgExists = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                        @Override
                        public Boolean run() throws SSLException {
                            return Boolean.valueOf(f_jsseHelper.doesSSLConfigExist(f_sslRef));
                        }
                    });

                } catch (PrivilegedActionException pae) {
                    Throwable cause = pae.getCause();
                    throw (SSLException) cause;
                }

                if (!sslCfgExists.booleanValue())
                    return null;
            }

            return SSLSupportService.getSSLSocketFactory(sslRef);
        } catch (SSLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SSL Exception with ssl ref id " + sslRef + ": " + e.toString());
            }
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception with ssl ref id " + sslRef + ": " + e.toString());
            }
            throw new IllegalStateException(e);
        }
    }

}
