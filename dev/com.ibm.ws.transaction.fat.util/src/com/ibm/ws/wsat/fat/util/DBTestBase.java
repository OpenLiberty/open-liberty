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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

public abstract class DBTestBase extends WSATTest {
	// Server Information
	public static final String clientName = "client";
	public static final String server1Name = "server1";
	public static final String server2Name = "server2";
	public static int server1Port = 8091;
	public static int server2Port = 8092;
	public static final String goodResult = "Success";
	public static String serverRollbackResult;
	public static String noTrans;

	// WS-AT Operation
	public static final String commit = "commit";
	public static final String rollback = "rollback";
	public static final String exception = "exception";
	public static final String setrollbackonly = "setrollbackonly";
	public static final String transcommit = "transcommit";

	public static String basicURL = "http://localhost";
	public static String CLient_URL;
	public static String Server1_URL;
	public static String Server2_URL;

	public static LibertyServer client;
	public static LibertyServer server1;
	public static LibertyServer server2;

	// Test URL
	// ATAssertion exists on Operation level in WSDL
	public static String appName;

	public static void initWSATTest(LibertyServer s) throws Exception {
		s.setServerStartTimeout(START_TIMEOUT);
		s.removeAllInstalledAppsForValidation();
		s.deleteDirectoryFromLibertyServerRoot("dropins");
	}

	public static void cleanupWSATTest(LibertyServer s) throws Exception {
		s.removeAllInstalledAppsForValidation();
		s.deleteDirectoryFromLibertyServerRoot("dropins");
	}

	public void commonTest(String appName, String testURL, String expectResult,
			String... expValule) {
		
		String resultURL = "/" + appName + "/ResultServlet";

		String clientResultURL = CLient_URL + resultURL + "?server="
				+ clientName;
		String clientInitURL = CLient_URL + resultURL + "?server="
				+ clientName + "&method=init";
		String server1ResultURL = Server1_URL + resultURL
				+ "?server=" + server1Name;
		String server1InitURL = Server1_URL + resultURL + "?server="
				+ server1Name + "&method=init";
		String server2ResultURL = Server2_URL + resultURL
				+ "?server=" + server2Name;
		String server2InitURL = Server2_URL + resultURL + "?server="
				+ server2Name + "&method=init";
		
		try {
			String initValue = "0";
			// Clean DB count to zero first
			InitDB(clientInitURL, clientName, initValue);
			InitDB(server1InitURL, server1Name, initValue);
			InitDB(server2InitURL, server2Name, initValue);

			String result = executeWSAT(testURL);
			assertTrue("Check result, expect is " + expectResult
					+ ", result is " + result, expectResult.equals(result));

			// Check WS-AT result
			if (expValule.length == 1) {
				CheckDB(clientResultURL, clientName, expValule[0]);
				CheckDB(server1ResultURL, server1Name, expValule[0]);
				CheckDB(server2ResultURL, server2Name, expValule[0]);
			} else if (expValule.length == 3) {
				CheckDB(clientResultURL, clientName, expValule[0]);
				CheckDB(server1ResultURL, server1Name, expValule[1]);
				CheckDB(server2ResultURL, server2Name, expValule[2]);
			} else {
				fail("Exception happens: Wrong expect value number: " + expValule.length);
			}
		} catch (Exception e) {
			Log.error(getClass(), "commonTest", e);
			fail("Exception happens: " + e.toString());
		}
	}

	public void InitDB(String url, String serverName, String value)
			throws Exception {
		HttpURLConnection con = getHttpConnection(new URL(url),
				HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
		BufferedReader br = HttpUtils.getConnectionStream(con);
		String result = br.readLine();
		Log.info(DBTestBase.class, "InitDB", "Init " + serverName + " DB from " + url + ": "
				+ result);
		assertTrue("Init " + serverName + " DB from "+url+", expect is 0, result is "
				+ result, result.equals(value));
	}

	public void CheckDB(String url, String serverName, String value)
			throws Exception {
		HttpURLConnection con = getHttpConnection(new URL(url),
				HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
		BufferedReader br = HttpUtils.getConnectionStream(con);
		String result = br.readLine();
		Log.info(DBTestBase.class, "CheckDB", "Check " + serverName + " DB from " + url + ": "
				+ result);
		assertTrue("Check " + serverName + " DB from "+url+", expect is " + value
				+ ", result is " + result, result.equals(value));
	}

	public String executeWSAT(String url) throws Exception {
		Log.info(DBTestBase.class, "executeWSAT", url);
		HttpURLConnection con = getHttpConnection(new URL(url),
				HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
		BufferedReader br = HttpUtils.getConnectionStream(con);
		String result = br.readLine();
		Log.info(DBTestBase.class, "executeWSAT", "Result: " + result);
		return result;
	}
}
