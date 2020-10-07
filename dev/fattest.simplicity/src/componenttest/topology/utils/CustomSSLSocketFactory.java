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
package componenttest.topology.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Class used for creating SSL connections to LDAP servers without needing to obtain or add the LDAP servers' signer
 * certificates to a truststore, considering a truststore likely isn't yet available at the time of the connection.
 */
public class CustomSSLSocketFactory extends SSLSocketFactory {
    private static final Class<?> c = CustomSSLSocketFactory.class;
    private SSLSocketFactory factory;
    private static String protocol = "TLS";

    public CustomSSLSocketFactory() {
        // Create trust manager that performs no checking of certificates
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        } };

        try {
            SSLContext sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            factory = sslContext.getSocketFactory();
        } catch (Exception e) {
            Log.error(c, "CustomSSLSocketFactory", e, "Error creating SSL context: " + e);
        }
    }

    public static SocketFactory getDefault() {
        return new CustomSSLSocketFactory();
    }

    @Override
    public Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return factory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return factory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return factory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return factory.createSocket(address, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return factory.createSocket(s, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    public static void setProtocol(String protocol) {
        CustomSSLSocketFactory.protocol = protocol;
    }

    public static void resetProtocol() {
        CustomSSLSocketFactory.protocol = "TLS";
    }
}
