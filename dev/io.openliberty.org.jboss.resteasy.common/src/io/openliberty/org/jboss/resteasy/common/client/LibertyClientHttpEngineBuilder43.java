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
package io.openliberty.org.jboss.resteasy.common.client;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngineBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43;

/**
 * This is primarily taken from org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43,
 * but modified to include proxy authentication support - changes noted with {@code //Liberty...}
 * comments.
 */
public class LibertyClientHttpEngineBuilder43 extends ClientHttpEngineBuilder43 {

    private ResteasyClientBuilder that;

    @Override
    public ClientHttpEngineBuilder resteasyClientBuilder(ResteasyClientBuilder resteasyClientBuilder) {
        that = resteasyClientBuilder;
        return super.resteasyClientBuilder(resteasyClientBuilder);
    }

    @Override
    protected ClientHttpEngine createEngine(final HttpClientConnectionManager cm, final RequestConfig.Builder rcBuilder,
                                            final HttpHost defaultProxy, final int responseBufferSize, final HostnameVerifier verifier, final SSLContext theContext) {
        final HttpClient httpClient;
        rcBuilder.setProxy(defaultProxy);
        if (System.getSecurityManager() == null) {
            httpClient = buildHttpClient(cm, rcBuilder, defaultProxy);
        } else {
            httpClient = AccessController.doPrivileged((PrivilegedAction<HttpClient>)() -> buildHttpClient(cm, rcBuilder, defaultProxy));
        }

        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient, true);
        engine.setResponseBufferSize(responseBufferSize);
        engine.setHostnameVerifier(verifier);
        // this may be null.  We can't really support this with Apache Client.
        engine.setSslContext(theContext);
        engine.setFollowRedirects(that.isFollowRedirects());
        return engine;
    }

    private HttpClient buildHttpClient(final HttpClientConnectionManager cm, final RequestConfig.Builder rcBuilder, HttpHost defaultProxy) {
        HttpClientBuilder httpClientBuilder= HttpClientBuilder.create()
                        .setConnectionManager(cm)
                        .setDefaultRequestConfig(rcBuilder.build())
                        .disableContentCompression();
        if (!that.isCookieManagementEnabled()) {
            httpClientBuilder.disableCookieManagement();
        }
        if (that.isDisableAutomaticRetries()) {
            httpClientBuilder.disableAutomaticRetries();
        }
        if (defaultProxy != null) {
            Object proxyUser = that.getConfiguration().getProperty(JAXRSClientConstants.PROXY_USERNAME);
            if (proxyUser != null) {
                Object proxyPass = that.getConfiguration().getProperty(JAXRSClientConstants.PROXY_PASSWORD);
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(defaultProxy),
                                             new UsernamePasswordCredentials(str(proxyUser), str(proxyPass)));
                httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
            }
        }
        return httpClientBuilder.build();
    }

    private static String str(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        return o.toString();
    }
}
