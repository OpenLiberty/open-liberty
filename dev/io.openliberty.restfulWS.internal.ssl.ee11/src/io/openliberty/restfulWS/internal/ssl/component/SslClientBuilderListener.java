/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.restfulWS.internal.ssl.component;

import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Optional;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.org.jboss.resteasy.common.client.JAXRSClientConstants;
import io.openliberty.restfulWS.client.ClientBuilderListener;

@Component(immediate = true, property = { "service.vendor=IBM" }, service = ClientBuilderListener.class)
public class SslClientBuilderListener implements ClientBuilderListener {

    private static JSSEHelper jsseHelper;

    @Reference(name = "SSLSupportService",
               service = SSLSupport.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSSLSupportService(SSLSupport service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "registerSSLSupportService");
        }
        jsseHelper = service.getJSSEHelper();
    }

    protected void unsetSSLSupportService(SSLSupport service) {
        jsseHelper = null;
    }

    @Override
    public void building(ClientBuilder clientBuilder) { // for JAX-RS clients
        Object sslRef = clientBuilder.getConfiguration().getProperty(JAXRSClientConstants.SSL_REFKEY);
        try {
            SSLContext sslContext = ((ResteasyClientBuilder) clientBuilder).getSSLContext();
            KeyStore keyStore = ((ResteasyClientBuilder) clientBuilder).getKeyStore();
            KeyStore trustStore = ((ResteasyClientBuilder) clientBuilder).getTrustStore();
            // don't override SSL config provided by the user
            if (sslContext == null && keyStore == null && trustStore == null) {
                getSSLContext(toRefString(sslRef)).ifPresent(clientBuilder::sslContext);
            }
        } catch (SSLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static Optional<SSLContext> getSSLContext(String sslRef) throws SSLException {
        if (jsseHelper == null) {
            return Optional.empty();
        }
        if (null == System.getSecurityManager()) {
            return Optional.of(jsseHelper.getSSLContext(sslRef, null, null));
        }
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Optional<SSLContext>>) () -> {
                return Optional.of(jsseHelper.getSSLContext(sslRef, null, null));
            });
        } catch (PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if (cause instanceof SSLException) {
                throw (SSLException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new SSLException((Exception) cause);
        }
    }

    static String toRefString(Object o) {
        if (o instanceof Supplier) {
            o = ((Supplier<?>)o).get();
        }
        if (o instanceof String) {
            return (String) o;
        }
        return o == null ? null : o.toString();
    }
}