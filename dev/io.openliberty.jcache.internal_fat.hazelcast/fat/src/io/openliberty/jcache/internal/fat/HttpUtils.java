/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.Header;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Some HTTP convenience methods.
 */
public class HttpUtils {
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

    /**
     * Make a POST call to an HTTPS REST endpoint.
     *
     * @param server                      The liberty server to send the call to.
     * @param endpoint                    The endpoint on the server to call.
     * @param expectedResponseStatus      The expected response HTTP return code.
     * @param expectedResponseContentType The expected response content type.
     * @param user                        The user to make the call with.
     * @param password                    The password to make the call with.
     * @return the response in the form of a string.
     * @throws Exception
     *                       If the call failed.
     */
    public static String performPost(LibertyServer server, String endpoint,
                                     int expectedResponseStatus, String expectedResponseContentType, String user,
                                     String password, String contentType, String content) throws Exception {
        final String methodName = "performPost()";

        try (CloseableHttpClient httpclient = HttpUtils.getInsecureHttpsClient(user, password)) {
            /*
             * Create a POST request to the Liberty server.
             */
            HttpPost httpPost = new HttpPost("https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
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
                HttpUtils.logHttpResponse(methodName, httpPost, response);

                StatusLine statusLine = response.getStatusLine();
                assertEquals("Unexpected status code response.", expectedResponseStatus, statusLine.getStatusCode());

                /*
                 * Check content type header.
                 */
                if (expectedResponseContentType != null) {
                    Header[] headers = response.getHeaders("content-type");
                    assertNotNull("Expected content type header.", headers);
                    assertEquals("Expected 1 content type header.", 1, headers.length);
                    assertEquals("Unexpected content type.", expectedResponseContentType, headers[0].getValue());
                }

                String contentString = EntityUtils.toString(response.getEntity());
                Log.info(HttpUtils.class, methodName, "HTTP post response contents: \n" + contentString);

                return contentString;
            }
        }
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
     * Log HTTP responses in a standard form.
     *
     * @param methodName The method name that is asking to log the response.
     * @param request    The request that was made.
     * @param response   The response that was received.
     */
    private static void logHttpResponse(String methodName, HttpRequestBase request,
                                        CloseableHttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        Log.info(HttpUtils.class, methodName, request.getMethod() + " " + request.getURI() + " ---> " + statusLine.getStatusCode()
                                              + " " + statusLine.getReasonPhrase());
    }

    /**
     * Get the server certificates that are in use by the remote endpoint.
     *
     * @param endpoint The endpoint to get the certificates from.
     * @return The certificate chain from the endpoint.
     * @throws Exception if there was an unforeseen error getting the certificates.
     */
    public static Certificate[] getServerCertificates(String endpoint) throws Exception {
        final String methodName = "getServerCertificates";

        /*
         * Make a request to the root context just to grab the certificate.
         */
        try (CloseableHttpClient httpclient = getInsecureHttpsClient(null, null)) {

            /*
             * Create a GET request to the Liberty server.
             */
            HttpGet httpGet = new HttpGet(endpoint);
            HttpContext context = new BasicHttpContext();

            /*
             * Send the GET request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpGet, context)) {
                logHttpResponse(HttpUtils.class, methodName, httpGet, response);

                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    /*
                     * We can still get the certificate even if we get a non-200 response.
                     */
                    Log.info(HttpUtils.class, methodName, "Expected response 200, but received response: " + statusLine + ". " + response);
                }

                /*
                 * Verify that the certificate is signed by the CA.
                 */
                Certificate[] certificates = (Certificate[]) context.getAttribute(PEER_CERTIFICATES);
                Log.info(HttpUtils.class, methodName, "Certificates: " + Arrays.toString(certificates));
                assertNotNull("Expected there to be TLS certificates in the HttpContext. Did the connection abort before we could retrieve them?",
                              certificates);
                return certificates;
            }
        }
    }

    /**
     * Log HTTP responses in a standard form.
     *
     * @param clazz
     *                       The class that is asking to log the response.
     * @param methodName
     *                       The method name that is asking to log the response.
     * @param request
     *                       The request that was made.
     * @param response
     *                       The response that was received.
     */
    public static void logHttpResponse(Class<?> clazz, String methodName, HttpRequestBase request,
                                       CloseableHttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        Log.info(clazz, methodName, request.getMethod() + " " + request.getURI() + " ---> " + statusLine.getStatusCode()
                                    + " " + statusLine.getReasonPhrase());
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
