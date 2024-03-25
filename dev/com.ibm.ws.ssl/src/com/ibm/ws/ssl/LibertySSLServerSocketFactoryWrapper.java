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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;

/*
 * A wrapper class that binds Liberty's SSL configuration in the {@code server.xml} to the
 * {@link Socket} in the {@code createSocket()} methods.
 *
 * This class should preserve the functionality of the current {@link SSLServerSocketFactory} and
 * only apply apply the config to the eventual {@link Socket}.
 */
public class LibertySSLServerSocketFactoryWrapper extends SSLServerSocketFactory {

    private static final TraceComponent tc = Tr.register(LibertySSLServerSocketFactoryWrapper.class, "SSL", "com.ibm.ws.ssl");

    // The actual SSLSocketFactory that does all of the work
    private SSLServerSocketFactory delegate = null;

    private final Properties props;

    public LibertySSLServerSocketFactoryWrapper(SSLServerSocketFactory sslSocketFactory) {
        delegate = sslSocketFactory;
        props = SSLPropertyUtils.lookupProperties();
    }

    public LibertySSLServerSocketFactoryWrapper(SSLServerSocketFactory sslSocketFactory, String alias) {
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
    public ServerSocket createServerSocket(int port) throws IOException {
        SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(port);
        SSLPropertyUtils.setSSLPropertiesOnServerSocket(props, socket);
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(port, backlog);
        SSLPropertyUtils.setSSLPropertiesOnServerSocket(props, socket);
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(port, backlog, ifAddress);
        SSLPropertyUtils.setSSLPropertiesOnServerSocket(props, socket);
        return socket;
    }
}
