/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

public abstract class WSATTest extends FATServletClient {

	public static String WSAT_NOT_INSTALLED = "WS-AT Feature is not installed";
	public static String FINISH_TWOWAY_MESSAGE = "Finish Twoway message";
	public static String WSAT_DETECTED = "Detected WS-AT policy, however there is no active transaction in current thread";

	@Rule public TestName testName = new TestName();
	
	// Normal number of seconds it takes a server to start up
	// Transaction timeouts will be adjusted if actual startup
	// time varies from this
	protected final static Duration normalStartTime = Duration.ofSeconds(12);

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

    public static void deleteStateFiles(LibertyServer... servers) throws Exception {
    	final String stateFile = XAResourceImpl.getStateFile().getName();
		Log.info(WSATTest.class, "deleteStateFiles", stateFile);
		for (LibertyServer server : servers) {
			server.deleteFileFromLibertyServerRoot(stateFile);
		}    	
    }
}