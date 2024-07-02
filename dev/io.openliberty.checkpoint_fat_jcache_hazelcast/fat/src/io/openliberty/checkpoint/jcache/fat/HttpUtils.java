/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.jcache.fat;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Some HTTP convenience methods.
 */
public class HttpUtils {
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

    /**
     * Get a (very) insecure HTTPs client that accepts all certificates and does
     * no host name verification.
     *
     * @param user     The user to make the call with.
     * @param password The password to make the call with.
     * @return The insecure HTTPS client.
     * @throws Exception If the client couldn't be created for some unforeseen reason.
     */
    public static CloseableHttpClient getInsecureHttpsClient(String user, String password) throws Exception {
        /*
         * Setup the basic authentication credentials.
         */
        CredentialsProvider credProvider = null;
        if (user != null && password != null) {
            credProvider = new BasicCredentialsProvider();
            credProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        }

        /*
         * Create an interceptor to pick off the TLS certificate from the remote endpoint.
         */
        HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
            if (context != null) {
                ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection) context
                                .getAttribute(HttpCoreContext.HTTP_CONNECTION);
                try {
                    SSLSession sslSession = routedConnection.getSSLSession();
                    if (sslSession != null) {

                        /*
                         * Get the server certificates from the SSLSession.
                         */
                        Certificate[] certificates = sslSession.getPeerCertificates();

                        /*
                         * Add the certificates to the context, where we can
                         * later grab it from
                         */
                        if (certificates != null) {
                            context.setAttribute(PEER_CERTIFICATES, certificates);
                        }
                    }
                } catch (ConnectionShutdownException e) {
                    /*
                     * This might occur when the request doesn't return a
                     * payload.
                     */
                    Log.warning(HttpUtils.class,
                                "Unable to save the connection's TLS certificates to the HTTP context since the connection was closed.");
                }
            }
        };

        /*
         * Create the insecure HTTPs client.
         */
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(MyTrustAllStrategy.INSTANCE).build();
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        return HttpClients.custom()
                        .setSSLSocketFactory(connectionFactory)
                        .setDefaultCredentialsProvider(credProvider)
                        .addInterceptorLast(certificateInterceptor)
                        .build();
    }

    /**
     * This replicates the TrustAllStrategy implemented in later versions of httpclient. We define our own so
     * earlier versions work.
     */
    private static class MyTrustAllStrategy implements TrustStrategy {
        public static final MyTrustAllStrategy INSTANCE = new MyTrustAllStrategy();

        @Override
        public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            return true;
        }
    }
}
