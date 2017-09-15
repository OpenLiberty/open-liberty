/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client;

import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl;
import org.apache.cxf.jaxrs.client.spec.TLSConfiguration;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 *
 */
public class JAXRSClientBuilderImpl extends ClientBuilderImpl {

    private final TLSConfiguration secConfig = new TLSConfiguration();

    public JAXRSClientBuilderImpl() {
        super();
    }

    @Override
    public Client build() {

        return new JAXRSClientImpl(super.getConfiguration(), secConfig);
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        secConfig.getTlsClientParams().setHostnameVerifier(verifier);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        secConfig.getTlsClientParams().setKeyManagers(null);
        secConfig.getTlsClientParams().setTrustManagers(null);
        secConfig.setSslContext(sslContext);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore store, char[] password) {
        secConfig.setSslContext(null);
        try {
            KeyManagerFactory tmf =
                            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(store, password);
            secConfig.getTlsClientParams().setKeyManagers(tmf.getKeyManagers());
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore store) {
        secConfig.setSslContext(null);
        try {
            TrustManagerFactory tmf =
                            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            secConfig.getTlsClientParams().setTrustManagers(tmf.getTrustManagers());
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }

        return this;
    }

    @Override
    public ClientBuilder property(String name, @Sensitive Object value) {
        // need to convert proxy password to ProtectedString
        if (JAXRSClientConstants.PROXY_PASSWORD.equals(name) && value != null &&
            !(value instanceof ProtectedString)) {
            return super.property(name, new ProtectedString(value.toString().toCharArray()));
        }
        return super.property(name, value);
    }
}
