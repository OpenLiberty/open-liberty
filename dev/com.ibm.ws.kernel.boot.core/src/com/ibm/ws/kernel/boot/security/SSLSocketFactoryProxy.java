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
package com.ibm.ws.kernel.boot.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

/**
 * This SSL SocketFactory is a proxy for other the Liberty SSL SocketFactory.
 * The SocketFactory infrastructure in the JDK uses Class.forName with the thread
 * context class loader, which inserts the actual SocketFactory class in the
 * initiating loader table. We use this proxy class because it is loaded by the
 * kernel class loader, which lasts for the duration of the JVM.
 */

public class SSLSocketFactoryProxy extends javax.net.ssl.SSLSocketFactory {
    private javax.net.ssl.SSLSocketFactory factory = null;
    private static com.ibm.ws.kernel.boot.security.SSLSocketFactoryProxy thisClass = null;

    public SSLSocketFactoryProxy() {

        Class<?> target;
        try {
            target = Thread.currentThread().getContextClassLoader().loadClass("com.ibm.ws.ssl.protocol.LibertySSLSocketFactory");
            factory = (SSLSocketFactory) target.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static javax.net.SocketFactory getDefault() {

        thisClass = new SSLSocketFactoryProxy();

        return thisClass;

    }

    @Override
    public String[] getDefaultCipherSuites() {
        String[] output = null;
        if (factory != null) {
            output = factory.getDefaultCipherSuites();
        }
        return output;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        String[] output = null;
        if (factory != null) {
            output = factory.getSupportedCipherSuites();
        }
        return output;
    }

    @Override
    public java.net.Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    @Override
    public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return factory.createSocket(s, host, port, autoClose);
    }

    @Override
    public java.net.Socket createSocket(InetAddress host, int port) throws IOException {
        return factory.createSocket(host, port);
    }

    @Override
    public java.net.Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return factory.createSocket(address, port, localAddress, localPort);
    }

    @Override
    public java.net.Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return factory.createSocket(host, port);
    }

    @Override
    public java.net.Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return factory.createSocket(host, port, localHost, localPort);
    }

}
