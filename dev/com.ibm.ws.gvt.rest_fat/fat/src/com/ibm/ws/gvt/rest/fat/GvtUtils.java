/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.gvt.rest.fat;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Some HTTP convenience methods.
 */
public class GvtUtils {

    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

    public enum HTTPRequestMethod {
        GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE
    }

    public static String performPostGvt(LibertyServer server, String endpoint,
                                        int expectedResponseStatus, String expectedResponseContentType, String user,
                                        String password, String contentType, String content) throws Exception {

        //return GvtUtils.postRequest(server, endpoint, expectedResponseStatus, expectedResponseContentType, user, password, contentType, content);

        final String methodName = "performPostGvt()";

        try (CloseableHttpClient httpclient = getInsecureHttpsClient(user, password)) {
            /*
             * Create a POST request to the Liberty server.
             */
            HttpPost httpPost = new HttpPost("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + endpoint);

            if (contentType != null && !contentType.isEmpty()) {
                httpPost.setHeader("Content-Type", contentType);
            }
            if (content != null && !content.isEmpty()) {
                httpPost.setEntity(new StringEntity(content));
            }

            /*
             * Send the POST request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                logHttpResponse(methodName, httpPost, response);

                /*
                 * Check statuscode.
                 */
                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedResponseStatus, statusLine.getStatusCode());

                String contentString = EntityUtils.toString(response.getEntity());

                return contentString;

            }
        }

    }

    /**
     * Log HTTP responses in a standard form.
     *
     * @param methodName The method name that is asking to log the response.
     * @param request    The request that was made.
     * @param response   The response that was received.
     */
    private static void logHttpResponse(String methodName, HttpRequestBase request,
                                        CloseableHttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        Log.info(GvtUtils.class, methodName, request.getMethod() + " " + request.getURI() + " ---> " + statusLine.getStatusCode()
                                             + " " + statusLine.getReasonPhrase());
    }

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
                    Log.warning(GvtUtils.class,
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

    public static HttpURLConnection getHttpConnectionForUTF(LibertyServer server) throws IOException {
        int timeout = 5000;
        URL url = createURL(server);
        HttpURLConnection connection = getHttpConnection(url, timeout, HTTPRequestMethod.GET);
        Log.info(GvtUtils.class, "getHttpConnection", "Connecting to " + url.toExternalForm() + " expecting http response in " + timeout + " seconds.");
        connection.connect();
        return connection;
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

    public static HttpURLConnection getHttpConnection(URL url, int timeout, HTTPRequestMethod requestMethod) throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod(requestMethod.toString());
        connection.setConnectTimeout(timeout);

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        return connection;
    }

    public static URL createURL(LibertyServer server) throws MalformedURLException {

        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort());
    }

}
