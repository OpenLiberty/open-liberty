/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat.tests;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Rule;
import org.junit.rules.TestName;

import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

public abstract class WSATTest {

	@Rule public TestName testName = new TestName();

	public final static int REQUEST_TIMEOUT = 60;

	private static final String testNameParameter = "testName";

    protected HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int connectionTimeout) throws IOException, ProtocolException, URISyntaxException {
    	

    	

    	
    	
    	
    	// Add testName parameter to query string so it appears in trace
    	final URI uri = appendUri(url.toURI(), testNameParameter + "=" + testName.getMethodName());
    	
    	System.out.println("xxxxxx: " + uri.toString());
    	
    	
    	
        return HttpUtils.getHttpConnection(uri.toURL(), expectedResponseCode, connectionTimeout, HTTPRequestMethod.GET);
    }

    private static URI appendUri(URI oldUri, String appendQuery) throws URISyntaxException {

        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;  
        }

        URI newUri = new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());

        return newUri;
    }
}
