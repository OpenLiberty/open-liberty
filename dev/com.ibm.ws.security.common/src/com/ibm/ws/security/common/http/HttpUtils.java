/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.http;

import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import com.ibm.websphere.ras.annotation.Sensitive;

public class HttpUtils {

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

    public HttpClient createHTTPClient(SSLSocketFactory sslSocketFactory, String url, boolean isHostnameVerification) {
        HttpClient client = null;
        if (url != null && url.startsWith("http:")) {
            client = HttpClientBuilder.create().build();
        } else {
            SSLConnectionSocketFactory connectionFactory = null;
            if (!isHostnameVerification) {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new AllowAllHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new StrictHostnameVerifier());
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
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new AllowAllHostnameVerifier());
            } else {
                connectionFactory = new SSLConnectionSocketFactory(sslSocketFactory, new StrictHostnameVerifier());
            }
            client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(connectionFactory).build();
        }
        return client;
    }

}
