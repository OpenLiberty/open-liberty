/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.security.internal;

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
public class JaxWsProxySSLSocketFactory extends SSLSocketFactory {

    private final String sslRef;

    private volatile SSLSocketFactory sslSocketFactory;

    private final Map<String, Object> extraProps = new HashMap<String, Object>();

    private boolean initilize() {
        if (null == sslSocketFactory) {
            synchronized (this) {
                if (null == sslSocketFactory) {
                    sslSocketFactory = JaxWsSSLManager.getSSLSocketFactoryBySSLRef(sslRef, extraProps, true);
                }
            }
        }
        // maybe the sslsupport service is not ready, still return the null
        return null != sslSocketFactory;
    }

    /**
     * @param sslRef
     */
    public JaxWsProxySSLSocketFactory(String sslRef, Map<String, Object> props) {
        super();
        this.sslRef = sslRef;
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
        } else {
            return null;
        }
    }

    @Override
    public Socket createSocket(String paramString, int paramInt) throws IOException, UnknownHostException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramString, paramInt);
        } else {
            return null;
        }
    }

    @Override
    public Socket createSocket(String paramString, int paramInt1, InetAddress paramInetAddress, int paramInt2) throws IOException, UnknownHostException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramString, paramInt1, paramInetAddress, paramInt2);
        } else {
            return null;
        }
    }

    @Override
    public Socket createSocket(InetAddress paramInetAddress, int paramInt) throws IOException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramInetAddress, paramInt);
        } else {
            return null;
        }
    }

    @Override
    public Socket createSocket(InetAddress paramInetAddress1, int paramInt1, InetAddress paramInetAddress2, int paramInt2) throws IOException {
        if (initilize()) {
            return sslSocketFactory.createSocket(paramInetAddress1, paramInt1, paramInetAddress2, paramInt2);
        } else {
            return null;
        }
    }

}
