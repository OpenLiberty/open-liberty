/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.fat;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.ChalltestsrvContainer;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test doesn't do anything but bring up a server that is running
 * acmeCA-2.0 feature for manual analysis. This class can be removed or replaced
 * when additional FATs are added.
 */
@RunWith(FATRunner.class)
public class AcmeConfigBringUp extends FATServletClient {

	private static final Class<?> c = AcmeConfigBringUp.class;

	@Server("com.ibm.ws.security.acme.fat.acmeconfigbringup")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		server.startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer();
	}

	@Test
	public void testBringUP() throws Exception {
		final String METHOD_NAME = "testBringUP";
		Log.info(c, METHOD_NAME, "Simple bring up test.");

		/*
		 * Verify the server started and the ACME challenge end-point is
		 * available.
		 */
		assertNotNull("Server did not start.", server.waitForStringInLog("CWWKF0011I"));
		assertNotNull("ACME challenge endpoint is not available.",
				server.waitForStringInLog("CWWKT0016I.*well-known/acme-challenge"));

		/*
		 * Make some requests to the ACME challenge end-point. There will be no
		 * authorizations in the AcmeProvider service, so we only expect error
		 * codes.
		 */
		postToChallengeEndpoint(METHOD_NAME, null, 404); // /.well-known/acme-challenge
		postToChallengeEndpoint(METHOD_NAME, "", 404); // /.well-known/acme-challenge/
		postToChallengeEndpoint(METHOD_NAME, "sometoken", 404); // /.well-known/acme-challenge/sometoken
	}

	/**
	 * Make a POST request to the ACME challenge end-point.
	 * 
	 * @param methodName
	 *            The name of the method making the call (for trace).
	 * @param token
	 *            The token to request the authorization for.
	 * @param expectCode
	 *            The HTTP code to expect in response.
	 * @throws Exception
	 *             if there was an issue making the call.
	 */
	private static void postToChallengeEndpoint(String methodName, String token, int expectCode) throws Exception {

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			/*
			 * Create a POST request.
			 */
			HttpPost httpPost;
			if (token == null) {
				httpPost = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
						+ AcmeConstants.ACME_CONTEXT_ROOT);
			} else {
				httpPost = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
						+ AcmeConstants.ACME_CONTEXT_ROOT + "/" + token);
			}

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(ChalltestsrvContainer.class, methodName, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != expectCode) {
					throw new IOException(methodName + ": Expected response " + expectCode + ", but received response: "
							+ statusLine);
				}
			}
		}
	}
}
