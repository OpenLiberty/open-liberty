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
package componenttest.topology.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class HttpsRequest extends HttpRequest {

    private static final Class<?> c = HttpsRequest.class;

    private final boolean isHTTPS;
    private boolean allowInsecure = false;
    private SSLSocketFactory sf = null;

    private static String concat(String... pathParts) {
        String base = "";
        for (String part : pathParts)
            base += part;
        return base;
    }

    public HttpsRequest(String url) {
        super(url);
        this.isHTTPS = url.startsWith("https://");
    }

    public HttpsRequest(LibertyServer server, String... pathParts) {
        this("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + concat(pathParts));
    }

    public HttpsRequest sslSocketFactory(SocketFactory sslSf) {
        if (!isHTTPS)
            throw new IllegalArgumentException("Cannot set an SSLSocketFactory on an HTTP connection");
        if (!(sslSf instanceof SSLSocketFactory))
            throw new IllegalArgumentException("Socket factory must be an instanceof SSLSocketFactory, but was: " + sslSf);
        this.sf = (SSLSocketFactory) sslSf;
        return this;
    }

    public HttpsRequest allowInsecure() {
        this.allowInsecure = true;
        return this;
    }

    @Override
    public HttpsRequest method(String method) {
        return (HttpsRequest) super.method(method);
    }

    @Override
    public HttpsRequest basicAuth(String user, String pass) {
        return (HttpsRequest) super.basicAuth(user, pass);
    }

    @Override
    public HttpsRequest expectCode(int expectedResponse) {
        return (HttpsRequest) super.expectCode(expectedResponse);
    }

    @Override
    public HttpsRequest jsonBody(String json) {
        return (HttpsRequest) super.jsonBody(json);
    }

    @Override
    public HttpsRequest requestProp(String key, String value) {
        return (HttpsRequest) super.requestProp(key, value);
    }

    @Override
    public HttpsRequest silent() {
        return (HttpsRequest) super.silent();
    }

    @Override
    public HttpsRequest timeout(int timeout) {
        return (HttpsRequest) super.timeout(timeout);
    }

    @Override
    void configureClient(HttpClient httpClient) {
        if (allowInsecure && sf != null) {
            throw new IllegalStateException("Cannot set allowInsecure=true and sslSocketFactory because " +
                                            " allowInsecure=true installs its own SSLSocketFactory.");
        }

        if (isHTTPS) {
            if (allowInsecure) {
                Log.info(c, "configureClient", "Allowing insecure connections for HTTPS client.");

                // Tell the Jakarta Commons Logging (JCL) library which logging implementation to use
                // (this line makes it possible to configure HttpClient logging with logging.properties from the JRE Logger)
                System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

                // Make it possible for HttpClient to handle https requests
                Protocol myhttps = new Protocol("https", new MyProtocolSocketFactory(), 443);
                Protocol.registerProtocol("https", myhttps);
            } else if (sf != null) {
                Log.info(c, "configureClient", "Using provided SSLSocketFactory for HTTPS client.");

                // Tell the Jakarta Commons Logging (JCL) library which logging implementation to use
                // (this line makes it possible to configure HttpClient logging with logging.properties from the JRE Logger)
                System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

                // Make it possible for HttpClient to handle https requests
                Protocol myhttps = new Protocol("https", new MyProtocolSocketFactory(sf), 443);
                Protocol.registerProtocol("https", myhttps);
            }

            // TODO Could throw exception in an else, but the previous logic didn't, so not risking for now.
        }
    }

    class MyProtocolSocketFactory implements ProtocolSocketFactory {
        private SSLContext sslContext = null;
        private SSLSocketFactory sf = null;

        /**
         * This constructor will create an insecure ProtocolSocketFactory that will trust all certificates.
         */
        MyProtocolSocketFactory() {
            super();
        }

        /**
         * This constructor will create a ProtocolSocketFactory that will use the passed in SSLSocketFactory.
         *
         * @param sf
         */
        MyProtocolSocketFactory(SSLSocketFactory sf) {
            super();
            this.sf = sf;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return (sf == null ? getSSLContext().getSocketFactory() : sf).createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return (sf == null ? getSSLContext().getSocketFactory() : sf).createSocket(host, port, localHost, localPort);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
                                   HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
            if (params == null) {
                throw new IllegalArgumentException("Parameters may not be null");
            }
            int timeout = params.getConnectionTimeout();
            SocketFactory socketfactory = (sf == null ? getSSLContext().getSocketFactory() : sf);
            if (timeout == 0) {
                return socketfactory.createSocket(host, port, localAddress, localPort);
            } else {
                Socket socket = socketfactory.createSocket();
                SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
                SocketAddress remoteaddr = new InetSocketAddress(host, port);
                socket.bind(localaddr);
                socket.connect(remoteaddr, timeout);
                return socket;
            }
        }

        private SSLContext getSSLContext() {
            if (this.sslContext == null) {
                this.sslContext = createSslContext();
            }
            return this.sslContext;
        }

        private SSLContext createSslContext() {
            try {
                SSLContext context = SSLContext.getInstance("SSL");
                context.init(
                             null,
                             new TrustManager[] { new InsecureTrustManager() },
                             new SecureRandom());
                return context;
            } catch (Exception e) {
                throw new HttpClientError(e.toString());
            }
        }
    }

    class InsecureTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Allow all.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Allow all.
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
