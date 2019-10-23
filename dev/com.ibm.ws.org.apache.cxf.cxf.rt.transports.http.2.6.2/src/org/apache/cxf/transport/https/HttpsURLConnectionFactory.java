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
import java.security.GeneralSecurityException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;


/**
 * This HttpsURLConnectionFactory implements the HttpURLConnectionFactory
 * for using the given SSL Policy to configure TLS connections for "https:"
 * URLs.
 * 
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
     * @param url   The target URL. This parameter must be an https url.
     * 
     * @return The HttpsURLConnection for the given URL.
     * @throws IOException This exception is thrown if 
     *         the "url" is not "https" or other IOException
     *         is thrown. 
     *                     
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

            Exception ex = null;
            try {
                decorateWithTLS(tlsClientParameters, connection);
            } catch (Exception e) {
                ex = e;
            } finally {
                if (ex != null) {
                    if (ex instanceof IOException) {
                        throw (IOException) ex;
                    }
                    // use exception.initCause(ex) to be java 5 compatible
                    IOException ioException = new IOException("Error while initializing secure socket");
                    ioException.initCause(ex);
                    throw ioException;
                }
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
            // ssl socket factory not yet instantiated, create a new one with tlsClientParameters's Trust
            // Managers, Key Managers, etc

            String provider = tlsClientParameters.getJsseProvider();

            String protocol = tlsClientParameters.getSecureSocketProtocol() != null ? tlsClientParameters
                .getSecureSocketProtocol() : "TLS";

            SSLContext ctx = provider == null ? SSLContext.getInstance(protocol) : SSLContext
                .getInstance(protocol, provider);
            ctx.getClientSessionContext().setSessionTimeout(tlsClientParameters.getSslCacheTimeout());
            KeyManager[] keyManagers = tlsClientParameters.getKeyManagers();
            if (tlsClientParameters.getCertAlias() != null) {
                getKeyManagersWithCertAlias(tlsClientParameters, keyManagers);
            }
            ctx.init(keyManagers, tlsClientParameters.getTrustManagers(),
                     tlsClientParameters.getSecureRandom());

            // The "false" argument means opposite of exclude.
            String[] cipherSuites = SSLUtils.getCiphersuites(tlsClientParameters.getCipherSuites(), SSLUtils
                .getSupportedCipherSuites(ctx), tlsClientParameters.getCipherSuitesFilter(), LOG, false);
            // The SSLSocketFactoryWrapper enables certain cipher suites
            // from the policy.
            socketFactory = new SSLSocketFactoryWrapper(ctx.getSocketFactory(), cipherSuites,
                                                        tlsClientParameters.getSecureSocketProtocol());
        } else {
           // ssl socket factory already initialized, reuse it to benefit of keep alive
        }
        
        
        HostnameVerifier verifier;
        if (tlsClientParameters.isUseHttpsURLConnectionDefaultHostnameVerifier()) {
            verifier = HttpsURLConnection.getDefaultHostnameVerifier();
        } else if (tlsClientParameters.isDisableCNCheck()) {
            verifier = CertificateHostnameVerifier.ALLOW_ALL;
        } else {
            verifier = CertificateHostnameVerifier.DEFAULT;
        }
        
        if (connection instanceof HttpsURLConnection) {
            // handle the expected case (javax.net.ssl)
            HttpsURLConnection conn = (HttpsURLConnection) connection;
            conn.setHostnameVerifier(verifier);
            conn.setSSLSocketFactory(socketFactory);
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
                Method getSSLSocketFactory =  connection.getClass().getMethod("getSSLSocketFactory");
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
                    c.setAccessible(true);
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
                //if we cannot set the SSLSocketFactor, we're in serious trouble.
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
    
    protected void getKeyManagersWithCertAlias(TLSClientParameters tlsClientParameters,
                                               KeyManager[] keyManagers) throws GeneralSecurityException {
        if (tlsClientParameters.getCertAlias() != null) {
            for (int idx = 0; idx < keyManagers.length; idx++) {
                if (keyManagers[idx] instanceof X509KeyManager) {
                    try {
                        keyManagers[idx] = new AliasedX509ExtendedKeyManager(
                            tlsClientParameters.getCertAlias(), (X509KeyManager)keyManagers[idx]);
                    } catch (Exception e) {
                        throw new GeneralSecurityException(e);
                    }
                }
            }
        }
    }

}



