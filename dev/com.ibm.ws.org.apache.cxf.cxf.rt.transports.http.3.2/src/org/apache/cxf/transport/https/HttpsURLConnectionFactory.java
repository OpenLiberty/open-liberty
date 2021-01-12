/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.https;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;


/**
 * This HttpsURLConnectionFactory implements the HttpURLConnectionFactory
 * for using the given SSL Policy to configure TLS connections for "https:"
 * URLs.
 */
public class HttpsURLConnectionFactory {

    /**
     * This constant holds the URL Protocol Identifier for HTTPS
     */
    public static final String HTTPS_URL_PROTOCOL_ID = "https";

    private static final Logger LOG =
        LogUtils.getL7dLogger(HttpsURLConnectionFactory.class);

    private static boolean weblogicWarned;

    /**
     * Cache the last SSLContext to avoid recreation
     */
    SSLSocketFactory socketFactory;
    // int lastTlsHash;  Liberty change: variable/line removed

    /**
     * This constructor initialized the factory with the configured TLS
     * Client Parameters for the HTTPConduit for which this factory is used.
     */
    public HttpsURLConnectionFactory() {
    }

    /**
     * Create a HttpURLConnection, proxified if necessary.
     *
     *
     * @param proxy This parameter is non-null if connection should be proxied.
     * @param url   The target URL.
     *
     * @return The HttpURLConnection for the given URL.
     * @throws IOException
     */
    public HttpURLConnection createConnection(TLSClientParameters tlsClientParameters,
            Proxy proxy, URL url) throws IOException {

        HttpURLConnection connection =
            (HttpURLConnection) (proxy != null
                                   ? url.openConnection(proxy)
                                   : url.openConnection());
        if (HTTPS_URL_PROTOCOL_ID.equals(url.getProtocol())) {

            if (tlsClientParameters == null) {
                tlsClientParameters = new TLSClientParameters();
            }

            try {
                decorateWithTLS(tlsClientParameters, connection);
            } catch (Throwable ex) {
                throw new IOException("Error while initializing secure socket", ex);
            }
        }

        return connection;
    }

    /**
     * This method assigns the various TLS parameters on the HttpsURLConnection
     * from the TLS Client Parameters. Connection parameter is of supertype HttpURLConnection,
     * which allows internal cast to potentially divergent subtype (https) implementations.
     */
    protected synchronized void decorateWithTLS(TLSClientParameters tlsClientParameters,
            HttpURLConnection connection) throws GeneralSecurityException {

        /* Liberty change: 5 lines below are removed with if clause
        int hash = tlsClientParameters.hashCode();
        if (hash != lastTlsHash) {
            lastTlsHash = hash;
            socketFactory = null;
        } Liberty change: end */

        // always reload socketFactory from HttpsURLConnection.defaultSSLSocketFactory and
        // tlsClientParameters.sslSocketFactory to allow runtime configuration change
        if (tlsClientParameters.isUseHttpsURLConnectionDefaultSslSocketFactory()) {
            socketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

        } else if (tlsClientParameters.getSSLSocketFactory() != null) {
            // see if an SSLSocketFactory was set. This allows easy interop
            // with not-yet-commons-ssl.jar, or even just people who like doing their
            // own JSSE.
            socketFactory = tlsClientParameters.getSSLSocketFactory();

        } else if (socketFactory == null) {

            SSLContext ctx = null;
            if (tlsClientParameters.getSslContext() != null) {
                // Use the SSLContext which was set
                ctx = tlsClientParameters.getSslContext();
            } else {
                // Create socketfactory with tlsClientParameters's Trust Managers, Key Managers, etc
                ctx = org.apache.cxf.transport.https.SSLUtils.getSSLContext(tlsClientParameters);
            }

            String[] cipherSuites =
                SSLUtils.getCiphersuitesToInclude(tlsClientParameters.getCipherSuites(),
                                                  tlsClientParameters.getCipherSuitesFilter(),
                                                  ctx.getSocketFactory().getDefaultCipherSuites(),
                                                  SSLUtils.getSupportedCipherSuites(ctx),
                                                  LOG);

            // The SSLSocketFactoryWrapper enables certain cipher suites from the policy.
            String protocol = tlsClientParameters.getSecureSocketProtocol() != null ? tlsClientParameters
                .getSecureSocketProtocol() : ctx.getProtocol();
            socketFactory = new SSLSocketFactoryWrapper(ctx.getSocketFactory(), cipherSuites,
                                                        protocol);
            //recalc the hashcode since some of the above MAY have changed the tlsClientParameters
            // lastTlsHash = tlsClientParameters.hashCode(); Liberty change: line removed
        } else {
           // ssl socket factory already initialized, reuse it to benefit of keep alive
        }


        HostnameVerifier verifier = org.apache.cxf.transport.https.SSLUtils
            .getHostnameVerifier(tlsClientParameters);

        if (connection instanceof HttpsURLConnection) {
            // handle the expected case (javax.net.ssl)
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            conn.setHostnameVerifier(verifier);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    conn.setSSLSocketFactory(socketFactory);
                    return null;
                } });
        } else {
            // handle the deprecated sun case and other possible hidden API's
            // that are similar to the Sun cases
            try {
                Method method = connection.getClass().getMethod("getHostnameVerifier");

                InvocationHandler handler = new ReflectionInvokationHandler(verifier) {
                    public Object invoke(Object proxy,
                                         Method method,
                                         Object[] args) throws Throwable {
                        try {
                            return super.invoke(proxy, method, args);
                        } catch (Exception ex) {
                            return false;
                        }
                    }
                };
                Object proxy = java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                        new Class[] {method.getReturnType()},
                                                                        handler);

                method = connection.getClass().getMethod("setHostnameVerifier", method.getReturnType());
                method.invoke(connection, proxy);
            } catch (Exception ex) {
                //Ignore this one
            }
            try {
                Method getSSLSocketFactory = connection.getClass().getMethod("getSSLSocketFactory");
                Method setSSLSocketFactory = connection.getClass()
                    .getMethod("setSSLSocketFactory", getSSLSocketFactory.getReturnType());
                if (getSSLSocketFactory.getReturnType().isInstance(socketFactory)) {
                    setSSLSocketFactory.invoke(connection, socketFactory);
                } else {
                    //need to see if we can create one - mostly the weblogic case.   The
                    //weblogic SSLSocketFactory has a protected constructor that can take
                    //a JSSE SSLSocketFactory so we'll try and use that
                    Constructor<?> c = getSSLSocketFactory.getReturnType()
                        .getDeclaredConstructor(SSLSocketFactory.class);
                    ReflectionUtil.setAccessible(c);
                    setSSLSocketFactory.invoke(connection, c.newInstance(socketFactory));
                }
            } catch (Exception ex) {
                if (connection.getClass().getName().contains("weblogic")) {
                    if (!weblogicWarned) {
                        weblogicWarned = true;
                        LOG.warning("Could not configure SSLSocketFactory on Weblogic.  "
                                    + " Use the Weblogic control panel to configure the SSL settings.");
                    }
                    return;
                }
                //if we cannot set the SSLSocketFactory, we're in serious trouble.
                throw new IllegalArgumentException("Error decorating connection class "
                        + connection.getClass().getName(), ex);
            }
        }
    }

    /*
     *  For development and testing only
     */
    protected void addLogHandler(Handler handler) {
        LOG.addHandler(handler);
    }

}
