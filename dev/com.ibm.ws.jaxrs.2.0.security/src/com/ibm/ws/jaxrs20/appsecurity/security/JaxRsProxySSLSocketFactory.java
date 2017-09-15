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
package com.ibm.ws.jaxrs20.appsecurity.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

/**
 * Use the proxy ssl socketFactory to delay the sslcontext initialization
 */
public class JaxRsProxySSLSocketFactory extends SSLSocketFactory {

    private final String sslRef;

    private volatile SSLSocketFactory sslSocketFactory;

    private final Map<String, Object> extraProps = new HashMap<String, Object>();

    private boolean initilize() {
        if (null == sslSocketFactory) {
            synchronized (this) {
                if (null == sslSocketFactory) {
                    sslSocketFactory = JaxRsSSLManager.getSSLSocketFactoryBySSLRef(sslRef, extraProps, false);
                }
            }
        }
        // maybe the sslsupport service is not ready, still return the null
        return null != sslSocketFactory;
    }

    /**
     * @param sslRef
     */
    public JaxRsProxySSLSocketFactory(String sslRef, Map<String, Object> props) {
        super();
        this.sslRef = sslRef;
        if (props != null)
            extraProps.putAll(props);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        if (initilize()) {
            return sslSocketFactory.getDefaultCipherSuites();
        } else {
            return null;
        }
    }

    @Override
    public String[] getSupportedCipherSuites() {
        if (initilize()) {
            return sslSocketFactory.getSupportedCipherSuites();
        } else {
            return null;
        }
    }

    @Override
    public Socket createSocket(Socket paramSocket, String paramString, int paramInt, boolean paramBoolean) throws IOException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramSocket, paramString, paramInt, paramBoolean);
        }

        throw new IOException("SSLSocketFactory creation fails as the SSL configuration reference \"" + sslRef + "\" is invalid.");

    }

    @Override
    public Socket createSocket(String paramString, int paramInt) throws IOException, UnknownHostException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramString, paramInt);
        }
        throw new IOException("SSLSocketFactory creation fails as the SSL configuration reference \"" + sslRef + "\" is invalid.");

    }

    @Override
    public Socket createSocket(String paramString, int paramInt1, InetAddress paramInetAddress, int paramInt2) throws IOException, UnknownHostException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramString, paramInt1, paramInetAddress, paramInt2);
        }
        throw new IOException("SSLSocketFactory creation fails as the SSL configuration reference \"" + sslRef + "\" is invalid.");

    }

    @Override
    public Socket createSocket(InetAddress paramInetAddress, int paramInt) throws IOException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramInetAddress, paramInt);
        }
        throw new IOException("SSLSocketFactory creation fails as the SSL configuration reference \"" + sslRef + "\" is invalid.");

    }

    @Override
    public Socket createSocket(InetAddress paramInetAddress1, int paramInt1, InetAddress paramInetAddress2, int paramInt2) throws IOException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramInetAddress1, paramInt1, paramInetAddress2, paramInt2);
        }
        throw new IOException("SSLSocketFactory creation fails as the SSL configuration reference \"" + sslRef + "\" is invalid.");
    }

}
