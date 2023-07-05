/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.common.ssl;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.ssl.SSLSupport;

@Component(service = SecuritySSLUtils.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class SecuritySSLUtils {

    public static final TraceComponent tc = Tr.register(SecuritySSLUtils.class);

    private static final String KEY_SSL_SUPPORT = "sslSupport";
    protected static volatile SSLSupport sslSupport;

    @Reference(name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC)
    public void setSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = sslSupportSvc;
    }

    public void unsetSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = null;
    }

    public static SSLSocketFactory getSSLSocketFactory(SSLSupport sslSupport, String sslConfigurationName) throws SSLException, NoSSLSocketFactoryException {
        SSLSocketFactory sslSocketFactory = null;
        if (sslSupport != null) {
            sslSocketFactory = sslSupport.getSSLSocketFactory(sslConfigurationName);
        }
        if (sslSocketFactory == null) {
            throw new NoSSLSocketFactoryException();
        }
        return sslSocketFactory;
    }

    public static SSLSocketFactory getSSLSocketFactory(SSLSupport sslSupport) throws SSLException, NoSSLSocketFactoryException {
        SSLSocketFactory sslSocketFactory = null;
        if (sslSupport != null) {
            sslSocketFactory = sslSupport.getSSLSocketFactory();
        }
        if (sslSocketFactory == null) {
            throw new NoSSLSocketFactoryException();
        }
        return sslSocketFactory;
    }

    public String getKeyStoreRef(String sslRef) {
        return getSslConfigProperty(sslRef, com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME);
    }

    public String getTrustStoreRef(String sslRef) {
        return getSslConfigProperty(sslRef, com.ibm.websphere.ssl.Constants.SSLPROP_TRUST_STORE_NAME);
    }

    String getSslConfigProperty(String sslRef, String propertyName) {
        String propertyValue = null;
        if (sslRef == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "sslRef not configured");
            }
            return null;
        }
        Properties sslConfigProps = getSslConfigProperties(sslRef);
        if (sslConfigProps != null) {
            propertyValue = sslConfigProps.getProperty(propertyName);
        }
        return propertyValue;
    }

    @FFDCIgnore(Exception.class)
    Properties getSslConfigProperties(String sslRef) {
        if (sslSupport == null) {
            return null;
        }
        Properties sslConfigProps;
        try {
            final Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
            sslConfigProps = (Properties) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return sslSupport.getJSSEHelper().getProperties(sslRef, connectionInfo, null, true);
                }
            });
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting SSL properties: " + e);
            }
            return null;
        }
        return sslConfigProps;
    }

}
