/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class DBWithoutAssertionTest extends DBTestBase {

	@BeforeClass
	public static void beforeTests() throws Exception {

		// Test URL
		appName = "wsatAppWithoutAssertion";

		// Basic URL
		client = LibertyServerFactory
				.getLibertyServer("WSATDB_Client");
		server1 = LibertyServerFactory
				.getLibertyServer("WSATDB_Server1");
		server2 = LibertyServerFactory
				.getLibertyServer("WSATDB_Server2");

		DBTestBase.initWSATTest(client);

		CLient_URL = "http://" + client.getHostname() + ":"
				+ client.getHttpDefaultPort();
		Server1_URL = "http://" + server1.getHostname() + ":"
				+ server1Port;
		Server2_URL = "http://" + server2.getHostname() + ":"
				+ server2Port;
		
		if (client != null && !client.isStarted()) {
			client.startServer();
		}
		if (server1 != null && !server1.isStarted()) {
			server1.startServer();
		}
		if (server2 != null && !server2.isStarted()) {
			server2.startServer();
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		ServerUtils.stopServer(client);
		ServerUtils.stopServer(server1);
		ServerUtils.stopServer(server2);

		DBTestBase.cleanupWSATTest(client);
	}
	@Before
	public void saveServerConfigs() throws Exception {
		client.saveServerConfiguration();
		server1.saveServerConfiguration();
		server2.saveServerConfiguration();
	}
	
	@After
	public void restoreServerConfigs() throws Exception {
		client.restoreServerConfiguration();
		server1.restoreServerConfiguration();
		server2.restoreServerConfiguration();
	}

	@Test
	public void test3DBsWithoutWSDL1_AllCommitByProxy() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void test3DBsWithoutWSDL2_ClientRollbackByProxy() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "="
				+ rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	public void test3DBsWithoutWSDL3_Server2RollbackByProxy() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "0");
	}

	@Test
	public void test3DBsWithoutWSDL4_AllCommitByProxyWithoutUserTransaction() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port + "&withouttrans=true";
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void test3DBsWithoutWSDL5_Server2RollbackByProxyWithoutUserTransaction() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ rollback + ":" + basicURL + ":"
				+ server2Port + "&withouttrans=true";
		commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "1", "1", "0");
	}
}
