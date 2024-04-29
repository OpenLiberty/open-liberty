/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;

/*
 * A wrapper class that binds Liberty's SSL configuration in the {@code server.xml} to the
 * {@link Socket} in the {@code createSocket()} methods.
 *
 * This class should preserve the functionality of the current {@link SSLSocketFactory} and
 * only apply apply the config to the eventual {@link Socket}.
 */
public class LibertySSLSocketFactoryWrapper extends SSLSocketFactory {

    private static final TraceComponent tc = Tr.register(LibertySSLSocketFactoryWrapper.class, "SSL", "com.ibm.ws.ssl");

    // The actual SSLSocketFactory that does all of the work
    private SSLSocketFactory delegate = null;

    private final Properties props;

    public LibertySSLSocketFactoryWrapper(SSLSocketFactory sslSocketFactory) {
        delegate = sslSocketFactory;
        props = SSLPropertyUtils.lookupProperties();
    }

    public LibertySSLSocketFactoryWrapper(SSLSocketFactory sslSocketFactory, String alias) {
        delegate = sslSocketFactory;
        props = SSLPropertyUtils.lookupProperties(alias);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        String securityLevel = props.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "securityLevel from properties is " + securityLevel);
        if (securityLevel == null)
            securityLevel = "HIGH";

        return Constants.adjustSupportedCiphersToSecurityLevel(delegate.getSupportedCipherSuites(), securityLevel);
    }

    @Override
    public Socket createSocket() throws IOException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket();
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }

    @Override
    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket(s, consumed, autoClose);
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket(s, host, port, autoClose);
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket(host, port);
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket(host, port, localHost, localPort);
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket(host, port);
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (javax.net.ssl.SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
        SSLPropertyUtils.setSSLPropertiesOnSocket(props, socket);
        return socket;
    }
}
