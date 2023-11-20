/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.fat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

public abstract class WSATTest {

	public static String WSAT_NOT_INSTALLED = "WS-AT Feature is not installed";
	public static String FINISH_TWOWAY_MESSAGE = "Finish Twoway message";
	public static String WSAT_DETECTED = "Detected WS-AT policy, however there is no active transaction in current thread";

	@Rule public TestName testName = new TestName();

	public final static int REQUEST_TIMEOUT = 60;
	public final static int START_TIMEOUT = 600000;

	private static final String testNameParameter = "testName";

    protected HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int connectionTimeout) throws IOException, ProtocolException, URISyntaxException {
    	// Add testName parameter to query string so it appears in trace
    	final URI uri = appendUri(url.toURI(), testNameParameter + "=" + testName.getMethodName());

    	return HttpUtils.getHttpConnection(uri.toURL(), expectedResponseCode, connectionTimeout, HTTPRequestMethod.GET);
    }

	protected HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int connectionTimeout, String testName) throws IOException, ProtocolException, URISyntaxException {

		// Add testName parameter to query string so it appears in trace
		final URI uri = appendUri(url.toURI(), testNameParameter + "=" + testName);

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

	public static void callClearResourcesServlet(String app, LibertyServer... servers) throws Exception{
		final String method = "callClearResourcesServlet";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "ClearResourcesServlet";

		for (LibertyServer server : servers) {
			String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + app + "/" + servletName;
	
			Log.info(WSATTest.class, method, "callClearResourcesServlet URL: " + urlStr);
			HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), 
				expectedConnectionCode, REQUEST_TIMEOUT);
			try {
				HttpUtils.getConnectionStream(con).readLine();
			} finally {
				con.disconnect();
			}
			
			server.setMarkToEndOfLog();
			server.setTraceMarkToEndOfDefaultTrace();
		}
	}
}