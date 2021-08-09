/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.consumer;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.openid4java.util.HttpFetcher;
import org.openid4java.util.HttpFetcherFactory;
import org.openid4java.util.HttpRequestOptions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openid20.OpenidClientConfig;

/**
 *
 */
public class OpenidHttpFetcherFactory extends HttpFetcherFactory {

    static final TraceComponent tc = Tr.register(OpenidHttpFetcherFactory.class);

    private OpenidClientConfig openidClientConfig = null;

    public OpenidHttpFetcherFactory(SSLContext sslContext, OpenidClientConfig openidClientConfig)
    {
        super(sslContext);
        this.openidClientConfig = openidClientConfig;
    }

    public OpenidHttpFetcherFactory(SSLContext sslContext, X509HostnameVerifier hostnameVerifier, OpenidClientConfig openidClientConfig)
    {
        super(sslContext, hostnameVerifier);
        this.openidClientConfig = openidClientConfig;
    }

    /**
     * Override this method, so we can set our own defaultOptions
     * (This will only have only one set of attributes for the
     * htmlResolver, YsdisResolver, xriResolver and ConsumerManager )
     **/
    @Override
    public HttpFetcher createFetcher(HttpRequestOptions defaultOptions)
    {
        defaultOptions.setSocketTimeout((int) openidClientConfig.getSocketTimeout());
        defaultOptions.setConnTimeout((int) openidClientConfig.getConnectTimeout());
        return super.createFetcher(defaultOptions);
    }
}
