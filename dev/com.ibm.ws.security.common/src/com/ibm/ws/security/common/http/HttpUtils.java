/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class HttpUtils {
    public static final TraceComponent tc = Tr.register(HttpUtils.class);

    public static enum RequestMethod {
        GET, POST
    };

    public HttpPost createHttpPostMethod(String url, final List<NameValuePair> commonHeaders) {
        if (url == null) {
            return null;
        }
        HttpPost postMethod = new HttpPost(url);
        addHeadersToHttpObject(postMethod, commonHeaders);
        return postMethod;
    }

    public HttpGet createHttpGetMethod(String url, final List<NameValuePair> commonHeaders) {
        if (url == null) {
            return null;
        }
        HttpGet getMethod = new HttpGet(url);
        addHeadersToHttpObject(getMethod, commonHeaders);
        return getMethod;
    }

    void addHeadersToHttpObject(HttpRequestBase requestObject, final List<NameValuePair> headers) {
        if (headers == null) {
            return;
        }
        for (Iterator<NameValuePair> i = headers.iterator(); i.hasNext();) {
            NameValuePair nvp = i.next();
            requestObject.addHeader(nvp.getName(), nvp.getValue());
        }
    }

    public HttpClient createHttpClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, String baUser, @Sensitive String baPassword) {

        HttpClient client = null;
        boolean addBasicAuthHeader = false;

        if (baUser != null && baPassword != null) {
            addBasicAuthHeader = true;
        }

        BasicCredentialsProvider credentialsProvider = null;
        if (addBasicAuthHeader) {
            credentialsProvider = createCredentialsProvider(baUser, baPassword);
        }

        client = createHttpClient(url.startsWith("https:"), isHostnameVerification, sslSocketFactory, addBasicAuthHeader, credentialsProvider);
        return client;

    }

    private HttpClient createHttpClient(boolean isSecure, boolean isHostnameVerification, SSLSocketFactory sslSocketFactory, boolean addBasicAuthHeader, BasicCredentialsProvider credentialsProvider) {
        HttpClient client = null;
        if (isSecure) {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new NoopHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new DefaultHostnameVerifier());
            }
            if (addBasicAuthHeader) {
                client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(connectionFactory).build();
            } else {
                client = HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).build();
            }
        } else {
            if (addBasicAuthHeader) {
                client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
            } else {
                client = HttpClientBuilder.create().build();
            }
        }
        return client;
    }

    private BasicCredentialsProvider createCredentialsProvider(String baUser, @Sensitive String baPassword) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(baUser, baPassword));
        return credentialsProvider;
    }

    @FFDCIgnore({ Exception.class })
    protected String getHTTPRequestAsString(HttpClient httpClient, String url) throws Exception {

        String json = null;
        try {
            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/json");
            HttpResponse result = null;
            try {
                result = httpClient.execute(request);
            } catch (IOException ioex) {
                String errMsg = "IOException: " + ioex.getMessage() + " " + ioex.getCause();
                String message = TraceNLS.getFormattedMessage(getClass(),
                        "com.ibm.ws.security.common.internal.resources.SSOCommonMessages", "OIDC_CLIENT_DISCOVERY_ERROR",
                        new Object[] { url, errMsg }, "Error processing discovery request");
                ;
                Tr.error(tc, message, new Object[0]);
                throw ioex;
            }
            StatusLine statusLine = result.getStatusLine();
            int iStatusCode = statusLine.getStatusCode();
            if (iStatusCode == 200) {
                json = EntityUtils.toString(result.getEntity(), "UTF-8");
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response: ", json);
                }
                if (json == null || json.isEmpty()) { // NO json response returned
                    throw new Exception(logErrorMessage(url, iStatusCode, "empty or null json response"));
                }
            } else {
                String errMsg = statusLine.getReasonPhrase();
                // error in getting the discovery response
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "status:" + iStatusCode + " errorMsg:" + errMsg);
                }
                throw new Exception(logErrorMessage(url, iStatusCode, errMsg));
            }
        } catch (Exception e) {
            throw e;
        }

        return json;
    }

    private String logErrorMessage(String url, int iStatusCode, String errMsg) {

        String defaultMessage = "Error processing discovery request";

        String message = TraceNLS.getFormattedMessage(getClass(),
                "com.ibm.ws.security.common.internal.resources.SSOCommonMessages", "OIDC_CLIENT_DISC_RESPONSE_ERROR",
                new Object[] { url, Integer.valueOf(iStatusCode), errMsg }, defaultMessage);
        ;
        Tr.error(tc, message, new Object[0]);
        return message;
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification) {
        HttpClient client = null;
        if (url != null && url.startsWith("http:")) {
            client = HttpClientBuilder.create().build();
        } else {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new NoopHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new DefaultHostnameVerifier());
            }
            client = HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).build();
        }
        return client;
    }

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, String baUser, @Sensitive String baPassword) {
        HttpClient client = null;

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(baUser, baPassword));

        if (url != null && url.toLowerCase().startsWith("http:")) {
            client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
        } else {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new NoopHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new DefaultHostnameVerifier());
            }
            client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(connectionFactory).build();
        }
        return client;
    }

    public String getHttpRequest(SSLSocketFactory sslSocketFactory, String discoveryUrl, boolean hostNameVerificationEnabled, String basicAuthIdentifier, String basicAuthSecret) throws Exception {

        HttpClient client = createHttpClient(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled, basicAuthIdentifier, basicAuthSecret);
        if (client != null) {
            return getHTTPRequestAsString(client, discoveryUrl);
        }
        return null;
    }

    public String invokeUrl(RequestMethod requestMethod, String url, SSLSocketFactory sslSocketFactory) throws Exception {
        try {
            HttpURLConnection connection = createConnection(requestMethod, url, sslSocketFactory);
            String response = readConnectionResponse(connection);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("Received unexpected " + responseCode + " response from " + requestMethod + " request sent to " + url + ".");
            }
            return response;
        } catch (IOException e) {
            throw new Exception("Connection to URL [" + url + "] failed. " + e, e);
        }
    }

    public HttpURLConnection createConnection(RequestMethod requestMethod, String url, SSLSocketFactory sslSocketFactory) throws IOException {
        HttpURLConnection connection = null;
        if (url != null && url.toLowerCase().startsWith("https")) {
            connection = getHttpsConnection(requestMethod, url, sslSocketFactory);
        } else {
            connection = getHttpConnection(requestMethod, url);
        }
        return connection;
    }

    public String readConnectionResponse(HttpURLConnection con) throws IOException {
        InputStream responseStream = getResponseStream(con);
        if (responseStream == null) {
            return null;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));
        String line;
        String response = "";
        while ((line = in.readLine()) != null) {
            response += line;
        }
        in.close();
        return response;
    }

    HttpsURLConnection getHttpsConnection(RequestMethod requestMethod, String apiUrl, SSLSocketFactory sslSocketFactory) throws IOException {
        URL url = new URL(apiUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslSocketFactory);
        connection.setRequestMethod(requestMethod.toString());
        return connection;
    }

    HttpURLConnection getHttpConnection(RequestMethod requestMethod, String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(requestMethod.toString());
        return connection;
    }

    InputStream getResponseStream(HttpURLConnection con) throws IOException {
        InputStream responseStream = null;
        int responseCode = con.getResponseCode();
        if (responseCode < 400) {
            responseStream = con.getInputStream();
            if (responseStream == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to obtain response stream from InputStream. Getting ErrorStream instead");
                }
                responseStream = con.getErrorStream();
            }
        } else {
            responseStream = con.getErrorStream();
            if (responseStream == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to obtain response stream from ErrorStream. Getting InputStream instead");
                }
                responseStream = con.getInputStream();
            }
        }
        return responseStream;
    }

    public HttpURLConnection setHeaders(HttpURLConnection con, @Sensitive Map<String, String> headers) {
        if (headers == null) {
            return con;
        }
        for (Entry<String, String> entry : headers.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }
        return con;
    }

}
