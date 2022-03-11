/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Some HTTP convenience methods.
 */
public class HttpUtils {

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
    private static CloseableHttpClient getInsecureHttpsClient(String user, String password) throws Exception {
        /*
         * Setup the basic authentication credentials.
         */
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));

        /*
         * Create the insecure HTTPs client.
         */
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        return HttpClients.custom().setSSLSocketFactory(connectionFactory).setDefaultCredentialsProvider(provider).build();
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
}
