/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.client.security;

import java.lang.reflect.Method;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.spec.TLSConfiguration;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

public class LibertyJaxRsClientSSLOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(LibertyJaxRsClientSSLOutInterceptor.class);

    private static final String HTTPS_SCHEMA = "https";

    private static final boolean overrideUserTLS = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

        @Override
        public Boolean run() {
            return Boolean.getBoolean("com.ibm.ws.jaxrs.client.security.overrideUserTLSConfig");
        }
    });

    private TLSConfiguration secConfig = null;

    /**
     * @param phase
     */
    public LibertyJaxRsClientSSLOutInterceptor(String phase) {
        super(phase);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        //SSL check
        //see if HTTPS is used
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        if (!address.startsWith(HTTPS_SCHEMA)) {
            return; // only process HTTPS requests
        }

        if (!overrideUserTLS && PropertyUtils.isTrue(message.get("org.apache.cxf.microprofile.client.sslConfigProvided"))) {
            return; // SSL config already provided
        }

        //see if SSL Ref id is used
        Object sslRefObj = message.get(JAXRSClientConstants.SSL_REFKEY);
        String sslRef = null;

        if (sslRefObj != null) {
            sslRef = (String) sslRefObj;
        }

        //Allow a null sslRef to be used,  Liberty will resolve it to the default
        // getSocketFactory will return null if either the ssl feature is not enabled
        // or if it is enabled but there is no SSL configuration defined.  A null here
        // means to use the JDK's SSL implementation.
        SSLSocketFactory socketFactory = getSSLSocketFactory(sslRef, message);
        if (socketFactory != null) {
            Object disableCNCheckObj = message.get(JAXRSClientConstants.DISABLE_CN_CHECK);
            Conduit cd = message.getExchange().getConduit(message);
            configClientSSL(cd, sslRef, PropertyUtils.isTrue(disableCNCheckObj), socketFactory, message);
        }
    }

    private void configClientSSL(Conduit conduit, String sslRef, boolean disableCNCheck, SSLSocketFactory socketFactory, Message message) {

        //for HTTPS protocol
        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = (HTTPConduit) conduit;

            TLSClientParameters tlsClientParams = retriveHTTPTLSClientParametersUsingSSLRef(httpConduit, sslRef, disableCNCheck, socketFactory, message);
            if (null != tlsClientParams) {
                httpConduit.setTlsClientParameters(tlsClientParams);
            }
        }
    }

    private TLSClientParameters retriveHTTPTLSClientParametersUsingSSLRef(HTTPConduit httpConduit, String sslRef, boolean disableCNCheck, SSLSocketFactory sslSocketFactory,
                                                                          Message message) {
        TLSClientParameters tlsClientParams = null;
        if (this.secConfig == null) {
            tlsClientParams = httpConduit.getTlsClientParameters();
        } else {
            tlsClientParams = this.secConfig.getTlsClientParams();
        }

        if (!StringUtils.isEmpty(sslRef)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Use the sslRef = " + sslRef + " to create the SSLSocketFactory.");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Get Liberty default SSLSocketFactory.");
            }
        }

        if (null != sslSocketFactory) {
            if (null == tlsClientParams) {
                tlsClientParams = new TLSClientParameters();
            }
            //let's use liberty SSL configuration
            tlsClientParams.setSSLSocketFactory(sslSocketFactory);
            tlsClientParams.setDisableCNCheck(disableCNCheck);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "May not enable feature ssl-1.0 or appSecurity-2.0.");
        }

        return tlsClientParams;
    }

    public void setTLSConfiguration(TLSConfiguration secConfig) {
        this.secConfig = secConfig;
    }

    private Class<?> getJaxRsSSLManagerClass() throws ClassNotFoundException {
        final Class<?> jaxrsSslMgrClass = Class.forName("com.ibm.ws.jaxrs20.appsecurity.security.JaxRsSSLManager");
        if (jaxrsSslMgrClass == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getSocketFactory could not find JaxRsSSLManager class");
            }
        }
        return jaxrsSslMgrClass;
    }

    private Object[] getJaxRsSSLManagerParams(String sslRef, Message message) {
        String uriString = (String) message.get(Message.REQUEST_URI);
        URI uri = URI.create(uriString);
        int port = uri.getPort();

        // if the port wasn't specified, use the default SSL port (443)
        if (port == -1 && !uriString.contains(":-1")) {
            Tr.debug(tc, "port was not specified, using the default SSL port (443)");
            port = 443;
        }

        Object[] parameters = { sslRef, uri.getHost(), Integer.toString(port) };
        return parameters;
    }

    private SSLSocketFactory getSSLSocketFactory(String sslRef, Message message) {
        try {
            final Class<?> jaxrsSslMgrClass = getJaxRsSSLManagerClass();
            Object classObject = jaxrsSslMgrClass.newInstance();
            Method m = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {

                @Override
                public Method run() throws NoSuchMethodException, SecurityException {
                    return jaxrsSslMgrClass.getDeclaredMethod("getSSLSocketFactoryBySSLRef", String.class, String.class, String.class);
                }
            });

            SSLSocketFactory ssLSocketFactory = (SSLSocketFactory) m.invoke(classObject, getJaxRsSSLManagerParams(sslRef, message));
            return ssLSocketFactory;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getSSLSocketFactoryBySSLRef reflection failed with exception " + e.toString());
            }
            return null;
        }
    }
}