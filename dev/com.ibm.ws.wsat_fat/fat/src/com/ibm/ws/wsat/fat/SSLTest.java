/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class SSLTest extends DBTestBase {

	@BeforeClass
	public static void beforeTests() throws Exception {

		// Test URL
		appName = "wsatApp";
		
		client = LibertyServerFactory
				.getLibertyServer("WSATSSL_Client");
		server1 = LibertyServerFactory
				.getLibertyServer("WSATSSL_Server1");
		server1.setHttpDefaultPort(server1Port);
		server2 = LibertyServerFactory
				.getLibertyServer("WSATSSL_Server2");
		server2.setHttpDefaultPort(server2Port);

		DBTestBase.initWSATTest(client);
		DBTestBase.initWSATTest(server1);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(client, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");
		ShrinkHelper.defaultDropinApp(server1, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");
		ShrinkHelper.defaultDropinApp(server2, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");

		CLient_URL = "http://" + client.getHostname() + ":"
				+ client.getHttpDefaultPort();
		Server1_URL = "http://" + server1.getHostname() + ":"
				+ server1.getHttpDefaultPort();
		Server2_URL = "http://" + server2.getHostname() + ":"
				+ server2.getHttpDefaultPort();

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
		DBTestBase.cleanupWSATTest(server1);
		DBTestBase.cleanupWSATTest(server2);
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
		client.waitForStringInLog("CWWKG001[78]I");
		server1.waitForStringInLog("CWWKG001[78]I");
		server2.waitForStringInLog("CWWKG001[78]I");
	}

	@Test
    @Mode(TestMode.LITE)
	public void testSSL_AllCommitByProxy() {
		client.waitForStringInLog("CWLIB0206I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testSSL_ClientRollbackByProxy() {
		server1.waitForStringInLog("CWLIB0206I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "="
				+ rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	public void testSSL_Server2RollbackByProxy() {
		server2.waitForStringInLog("CWLIB0206I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "0");
	}

	@Test
  @Mode(TestMode.LITE)
	public void testSSL_AllCommitByProx_WithClientAuth() throws Exception {
		client.waitForStringInLog("CWLIB0206I");
		client.setMarkToEndOfLog();
		client.setServerConfigurationFile("ssl/server_client.xml");
		server1.setMarkToEndOfLog();
		server1.setServerConfigurationFile("ssl/server_server1.xml");
		server2.setMarkToEndOfLog();
		server2.setServerConfigurationFile("ssl/server_server2.xml");
		client.waitForStringInLogUsingMark("CWWKG0017I");
		server1.waitForStringInLogUsingMark("CWWKG0017I");
		server2.waitForStringInLogUsingMark("CWWKG0017I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testSSL_ClientRollbackByProxy_WithClientAuth() throws Exception {
		server1.waitForStringInLog("CWLIB0206I");
		client.setMarkToEndOfLog();
		client.setServerConfigurationFile("ssl/server_client.xml");
		server1.setMarkToEndOfLog();
		server1.setServerConfigurationFile("ssl/server_server1.xml");
		server2.setMarkToEndOfLog();
		server2.setServerConfigurationFile("ssl/server_server2.xml");
		client.waitForStringInLogUsingMark("CWWKG0017I");
		server1.waitForStringInLogUsingMark("CWWKG0017I");
		server2.waitForStringInLogUsingMark("CWWKG0017I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "="
				+ rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	public void testSSL_Server2RollbackByProxy_WithClientAuth() throws Exception {
		server2.waitForStringInLog("CWLIB0206I");
		client.setMarkToEndOfLog();
		client.setServerConfigurationFile("ssl/server_client.xml");
		server1.setMarkToEndOfLog();
		server1.setServerConfigurationFile("ssl/server_server1.xml");
		server2.setMarkToEndOfLog();
		server2.setServerConfigurationFile("ssl/server_server2.xml");
		client.waitForStringInLogUsingMark("CWWKG0017I");
		server1.waitForStringInLogUsingMark("CWWKG0017I");
		server2.waitForStringInLogUsingMark("CWWKG0017I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + server2Name + "p="
				+ rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "0");
	}
}
