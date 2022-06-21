/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

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

    public HttpClient createHttpClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification) {
        return createHttpClient(sslSocketFactory, url, isHostnameVerification, false);
    }

    public HttpClient createHttpClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections) {
        boolean isSecure = (url != null && url.startsWith("https:"));
        return createHttpClient(sslSocketFactory, isSecure, isHostnameVerification, useSystemPropertiesForHttpClientConnections, null);
    }
    
    private HttpClient createHttpClient(SSLSocketFactory sslSocketFactory, boolean isSecure, boolean isHostnameVerification, boolean useSystemPropertiesForHttpClientConnections, BasicCredentialsProvider credentialsProvider) {
        HttpClient client = null;
        if (isSecure) {
            ClassLoader origCL = ThreadContextHelper.getContextClassLoader();
            ThreadContextHelper.setClassLoader(getClass().getClassLoader());
            try {
                SSLConnectionSocketFactory connectionFactory = null;
                if (!isHostnameVerification) {
                    connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new NoopHostnameVerifier());
                } else {
                    connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new DefaultHostnameVerifier());
                }
                HttpClientBuilder clientBuilder = createBuilder(useSystemPropertiesForHttpClientConnections).setSSLSocketFactory(connectionFactory);
                if (credentialsProvider != null) {
                    clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
                client = clientBuilder.build();
            } finally {
                ThreadContextHelper.setClassLoader(origCL);
            }
        } else {
            HttpClientBuilder clientBuilder = createBuilder(useSystemPropertiesForHttpClientConnections);
            if (credentialsProvider != null) {
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            client = clientBuilder.build();
        }
        return client;
    }
    
    HttpClientBuilder createBuilder(boolean useSystemProperties) {
        return useSystemProperties ? HttpClientBuilder.create().useSystemProperties() : HttpClientBuilder.create();
    }

    public String getHttpJsonRequest(SSLSocketFactory sslSocketFactory, String url, boolean hostNameVerificationEnabled, boolean useSystemProperties) throws Exception {
        HttpClient client = createHttpClient(sslSocketFactory, url, hostNameVerificationEnabled, useSystemProperties);
        if (client != null) {
            return getHttpJsonRequestAsString(client, url);
        }
        return null;
    }

    String getHttpJsonRequestAsString(HttpClient httpClient, String url) throws Exception {
        List<NameValuePair> headers = new ArrayList<>();
        headers.add(new BasicNameValuePair("Content-Type", "application/json"));

        return getHttpRequestAsString(httpClient, url, headers);
    }

    String getHttpRequestAsString(HttpClient httpClient, String url, List<NameValuePair> headers) throws Exception {
        HttpGet request = createHttpGetMethod(url, headers);
        HttpResponse result = null;
        
        ClassLoader origCL = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(getClass().getClassLoader());
        try {
            result = httpClient.execute(request);
        } finally {
            ThreadContextHelper.setClassLoader(origCL);
        }
        return extractResponseAsString(result, url);
    }

    String extractResponseAsString(HttpResponse result, String url) throws IOException, HttpResponseNullOrEmptyException, HttpResponseNot200Exception {
        StatusLine statusLine = result.getStatusLine();
        int iStatusCode = statusLine.getStatusCode();
        if (iStatusCode == 200) {
            String response = EntityUtils.toString(result.getEntity(), "UTF-8");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Response: ", response);
            }
            if (response == null || response.isEmpty()) {
                throw new HttpResponseNullOrEmptyException(url, iStatusCode, "empty or null response");
            }
            return response;
        } else {
            String errMsg = statusLine.getReasonPhrase();
            // error in getting the response
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "status:" + iStatusCode + " errorMsg:" + errMsg);
            }
            throw new HttpResponseNot200Exception(url, iStatusCode, errMsg);
        }
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
