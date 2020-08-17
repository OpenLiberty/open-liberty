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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * ========== Test Case Readme ==========
 * more detail information, please take a look the index page of the wsatApp
 * There are 3 servers: client, server1, server2
 * 
 * test3DBs01_AllCommitByProxy: client commit, server1 commit, server2 commit
 * 
 * test3DBs02_AllRollbackByProxy: client rollback, server1 rollback, server2 rollback
 * 
 * test3DBs03_ClientRollbackByProxy: client rollback, server1 commit, server2 commit
 * 
 * test3DBs44_ClientExceptionByProxy: client throw exception, server1 commit, server2 commit
 * 
 * test3DBs05_ClientSetRollbackOnlyByProxy: client setRollbackOnly, server1 commit, server2 commit
 * 
 * test3DBs06_Server1RollbackByProxy: client commit, server1 rollback, server2 commit
 * 
 * test3DBs07_Server2RollbackByProxy: client commit, server1 commit, server2 rollback
 * 
 * 
 * 
 * test3DBs08_AllCommitByProxy_With2SameServers: client commit, server1 commit, server1 commit again
 * 
 * test3DBs09_ClientRollbackByProxy_With2SameServers: client rollback, server1 commit, server1 commit again
 * 
 * test3DBs10_Server2RollbackByProxy_With2SameServers: client commit, server1 commit, server1 rollback
 * 
 * test3DBs11_AllCommitByProxy_WithNonGlobalTrans: client commit, server1 commit, server2 do a non-global transaction
 * 
 * test3DBs12_ClientRollbackByProxy_WithNonGlobalTrans: client rollback, server1 commit, server2 do a non-global transaction
 * 
 * test3DBs13_Server2RollbackByProxy_WithNonGlobalTrans: client commit, server1 rollback, server2 do a non-global transaction
 * 
 x test3DBs14_NestedTransByProxy: client commit, server1 commit by using its own tansaction, server2 commit
 
 * test3DBs15_AllCommitByProxyWithoutUserTransaction: client commit, server1 commit, server2 commit without transaction
 * 
 * test3DBs16_AllSayHelloByProxyWithoutUserTransaction: client commit, server1 commit, server2 commit without transaction without WS-AT
 * 
 * 
 * 
 * test3DBs17_A_B_C_AllCommitByProxy: A->B->C, A->C, all commit
 * 
 * test3DBs18_A_B_C_ClientRollbackByProxy: A->B->C, A->C, client rollback
 * 
 * test3DBs19_A_B_C_NestServer2RollbackByProxy: A->B->C, A->C, first C rollback
 * 
 * test3DBs20_A_B_C_Server2RollbackByProxy: A->B->C, A->C, second C rollback
 * 
 * test3DBs21_A_B_A_AllCommitByProxy: A->B->A, A->C, all commit
 * 
 * test3DBs22_A_B_A_ClientRollbackByProxy: A->B->A, A->C, client rollback
 * 
 * test3DBs23_A_B_A_NestClientRollbackByProxy: A->B->A, A->C, first A rollback
 * 
 * test3DBs24_A_B_A_Server2RollbackByProxy: A->B->A, A->C, second A rollback
 * 
 * 
 * 
 * test3DBs25_AllCommitByLocalWSDL: client commit, server1 commit, server2 commit, by using local WSDL file
 * 
 * test3DBs26_Server1RollbackByLocalWSDL: client rollback, server1 commit, server2 commit, by using local WSDL file
 * 
 * test3DBs27_AllCommitByLocalWSDLWithoutUserTransaction: client commit, server1 commit, server2 commit, by using local WSDL file and without transaction
 * 
 x test3DBs28_AllCommitByDispatch: client commit, server1 commit, server2 commit, by using Dispatch way
 *
 x test3DBs29_Server2RollbackByDispatch: client commit, server1 commit, server2 rollback, by using Dispatch way
 *
 *
 *
 * test3DBs30_AllCommitWithProxyServerByProxy: client commit, server1 commit, server2 commit, by using proxy
 * 
 * test3DBs31_ClientRollbackWithProxyServerByProxy: client rollback, server1 commit, server2 commit, by using proxy
 * 
 * test3DBs32_Server2SetRollbackOnlyByLocalWSDL: client commit, server1 commit, server2 setrollbackonly, by using local wsdl
 * 
 * 
 * 
 * test3DBs33_Server1UOWCommitByProxy: client commit, server1 UOW commit, server2 rollback, by using proxy
 * 
 * test3DBs34_Server2UOWSetRollbackOnlyByProxy: client commit, server1 commit, server2 UOW setrollbackonly, by using proxy
 * 
 * 
 * 
 * test3DBs35: client commit, server1 commit without operation ATAssertion, server2 commit with userTransaction
 * 
 * test3DBs36: client commit, server1 commit, server2 commit without operation ATAssertion with userTransaction
 * 
 * test3DBs37: client commit, server1 commit without operation ATAssertion, server2 rollback commit with userTransaction
 * 
 * test3DBs38: client commit, server1 commit, server2 rollback without operation ATAssertion with userTransaction
 * 
 * 
 * test3DBs39: client commit, server1 commit without operation ATAssertion, server2 commit without userTransaction
 * 
 * test3DBs40: client commit, server1 commit, server2 commit without operation ATAssertion without userTransaction
 * 
 * test3DBs41: client commit, server1 commit without operation ATAssertion, server2 rollback without userTransaction
 * 
 * test3DBs42: client commit, server1 commit, server2 rollback without operation ATAssertion without userTransaction
 * 
 * 
 * test3DBs43: client commit, server1 commit without optional operation ATAssertion, server2 commit with userTransaction
 * 
 * test3DBs44: client commit, server1 commit, server2 commit without optional operation ATAssertion with userTransaction
 * 
 * test3DBs45: client commit, server1 commit without optional operation ATAssertion, server2 rollback commit with userTransaction
 * 
 * test3DBs46: client commit, server1 commit, server2 rollback without optional operation ATAssertion with userTransaction
 * 
 * 
 * test3DBs47: client commit, server1 commit without optional operation ATAssertion, server2 commit without userTransaction
 * 
 * test3DBs48: client commit, server1 commit, server2 commit without optional operation ATAssertion without userTransaction
 * 
 * test3DBs49: client commit, server1 commit without optional operation ATAssertion, server2 rollback without userTransaction
 * 
 * test3DBs50: client commit, server1 commit, server2 rollback without optional operation ATAssertion without userTransaction
 * 
 * 
 * test3DBs51: client commit, server1 commit, server2 commit with service ATAssertion with userTransaction
 * 
 * test3DBs52: client commit, server1 commit, server2 rollback with service ATAssertion with userTransaction
 *
 * test3DBs53: client commit, server1 commit, server2 commit with service ATAssertion without userTransaction
 * 
 * 
 * test3DBs54: client commit, server1 commit, server2 commit with optional service ATAssertion with userTransaction
 * 
 * test3DBs55: client commit, server1 commit, server2 rollback with optional service ATAssertion with userTransaction
 * 
 * test3DBs56: client commit, server1 commit, server2 commit with optional service ATAssertion without userTransaction
 * 
 * test3DBs57: client commit, server1 commit, server2 rollback with optional service ATAssertion without userTransaction
 * 
 * test3DBs58: client commit, server1 commit, server2 rollback and client commit in its catch
 * 
 * 
 * 
 * 
 * DBWithoutAssertionTest: Test without WSDL file, only use WS-AT feature and userTransaction to enable WS-AT global transaction
 * 
 */

@RunWith(FATRunner.class)
public class DBTest extends DBTestBase {

	@BeforeClass
	public static void beforeTests() throws Exception {

		// Server Information
		serverRollbackResult = "Throw exception for rollback from server side!";
		noTrans = "Detected WS-AT policy, however there is no active transaction in current thread.";

		// Basic URL
		client = LibertyServerFactory
				.getLibertyServer("WSATDB_Client");
		server1 = LibertyServerFactory
				.getLibertyServer("WSATDB_Server1");
		server1.setHttpDefaultPort(server1Port);
		server2 = LibertyServerFactory
				.getLibertyServer("WSATDB_Server2");
		server2.setHttpDefaultPort(server2Port);

		DBTestBase.initWSATTest(client);
		DBTestBase.initWSATTest(server1);
		DBTestBase.initWSATTest(server2);

		CLient_URL = "http://" + client.getHostname() + ":"
				+ client.getHttpDefaultPort();
		Server1_URL = "http://" + server1.getHostname() + ":"
				+ server1.getHttpDefaultPort();
		Server2_URL = "http://" + server2.getHostname() + ":"
				+ server2.getHttpDefaultPort();

		// Test URL
		// ATAssertion exists on Operation level in WSDL
		appName = "wsatApp";

		ShrinkHelper.defaultDropinApp(client, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");
		ShrinkHelper.defaultDropinApp(server1, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");
		ShrinkHelper.defaultDropinApp(server2, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");

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
	public void test3DBs01_AllCommitByProxy() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	@Mode(TestMode.FULL)
	public void test3DBs02_AllRollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ rollback + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, "Throw exception for rollback from server side!",
				"0");
	}

	@Test
	public void test3DBs03_ClientRollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}

	@Test
	public void test3DBs04_ClientExceptionByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + exception;
		commonTest(appName, wsatURL, "Throw new RuntimeException from client side!", "0");
	}

	@Test
	@Mode(TestMode.FULL)
	public void test3DBs05_ClientSetRollbackOnlyByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + setrollbackonly;
		commonTest(appName, wsatURL, goodResult, "0");
	}

	@Test
	public void test3DBs06_Server1RollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ rollback + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}

	@Test
	@Mode(TestMode.FULL)
	public void test3DBs07_Server2RollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs08_AllCommitByProxy_With2SameServers() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server1Name + "l=" + commit + ":" + basicURL + ":"
				+ server1Port;
		commonTest(appName, wsatURL, goodResult, "1", "2", "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs09_ClientRollbackByProxy_With2SameServers() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server1Name + "l=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs10_Server2RollbackByProxy_With2SameServers() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server1Name + "l=" + rollback + ":" + basicURL + ":"
				+ server1Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	public void test3DBs11_AllCommitByProxy_WithNonGlobalTrans() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server1Name + "p=:" + basicURL + ":"
				+ server1Port;
		commonTest(appName, wsatURL, goodResult, "1", "1", "0");
	}
	
	@Test
	public void test3DBs12_ClientRollbackByProxy_WithNonGlobalTrans() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server1Name + "p=:" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs13_Server2RollbackByProxy_WithNonGlobalTrans() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ rollback + ":" + basicURL + ":" + server1Port + "&"
				+ server1Name + "p=:" + basicURL + ":"
				+ server1Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}

	//@Test
	// Comment this test first because exception will fail all tests
	@AllowedFFDC("javax.transaction.NotSupportedException")
	public void test3DBs14_NestedTransByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ transcommit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, "Nested transactions are not supported.", "0");
	}

	@Test
	@Mode(TestMode.FULL)
	public void test3DBs15_AllCommitByProxyWithoutUserTransaction() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&withouttrans=true";
		commonTest(appName, wsatURL, noTrans, "1", "0", "0");
	}

	@Test
	@Mode(TestMode.FULL)
	public void test3DBs16_AllSayHelloByProxyWithoutUserTransaction() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p=" + ":"
				+ basicURL + ":" + server1Port + "&" + server2Name + "p=" + ":"
				+ basicURL + ":" + server2Port + "&withouttrans=true";
		commonTest(appName, wsatURL, goodResult, "1", "0", "0");
	}
	
	// Fix later begin: It does't work when A->B->C and A->B->A by Tim
	// Already fix in 192130
	
	@Test
	public void test3DBs17_A_B_C_AllCommitByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + commit + "-" + server2Name + "-" + server2Port +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1", "1", "2");
	}
	
	@Test
	public void test3DBs18_A_B_C_ClientRollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + commit + "-" + server2Name + "-" + server2Port +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs19_A_B_C_NestServer2RollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + rollback + "-" + server2Name + "-" + server2Port +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	public void test3DBs20_A_B_C_Server2RollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + commit + "-" + server2Name + "-" + server2Port +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs21_A_B_A_AllCommitByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + commit + "-" + clientName + "-" + client.getHttpDefaultPort() +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "2", "1", "1");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs22_A_B_A_ClientRollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + commit + "-" + clientName + "-" + client.getHttpDefaultPort() +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs23_A_B_A_NestClientRollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + rollback + "-" + clientName + "-" + client.getHttpDefaultPort() +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs24_A_B_A_Server2RollbackByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ "nested-" + commit + "-" + clientName + "-" + client.getHttpDefaultPort() +":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	// Fix later end: It does't work when A->B->C and A->B->A by Tim
	// Already fix in 192130

	@Test
	public void test3DBs25_AllCommitByLocalWSDL() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "l="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "l=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void test3DBs26_Server1RollbackByLocalWSDL() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "l="
				+ rollback + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "l=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}

	@Test
	@Mode(TestMode.FULL)
	public void test3DBs27_AllCommitByLocalWSDLWithoutUserTransaction() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "l="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "l=" + commit + ":" + basicURL + ":"
				+ server2Port + "&withouttrans=true";
		commonTest(appName, wsatURL, noTrans, "1", "0", "0");
	}

	// Will add Dispatch test in future by Jordan
	// @Test
	public void test3DBs28_AllCommitByDispatch() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "d="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "d=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	// Will add Dispatch test in future by Jordan
	// @Test
	public void test3DBs29_Server2RollbackByDispatch() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "d="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "d=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "0");
	}

	@Test
	public void test3DBs30_AllCommitWithProxyServerByProxy() throws Exception {
		client.setServerConfigurationFile("proxy/server_client.xml");
		server1.setServerConfigurationFile("proxy/server_server1.xml");
		server2.setServerConfigurationFile("proxy/server_server2.xml");

		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void test3DBs31_ClientRollbackWithProxyServerByProxy() throws Exception {
		client.setServerConfigurationFile("proxy/server_client.xml");
		server1.setServerConfigurationFile("proxy/server_server1.xml");
		server2.setServerConfigurationFile("proxy/server_server2.xml");

		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	public void test3DBs32_Server2SetRollbackOnlyByLocalWSDL() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "l="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "m=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, "null", "0");
	}
	
	@Test
	public void test3DBs33_Server1UOWCommitByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "q="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0", "1", "0");
	}
	
	@Test
	@AllowedFFDC("java.lang.IllegalStateException")
	public void test3DBs34_Server2UOWSetRollbackOnlyByProxy() {
		final String testURL = "/" + appName + "/ClientServlet";

		String wsatURL = CLient_URL + testURL + "?" + server1Name + "l="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "r=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, "No transaction associated with this thread", "0", "0", "1");
	}
	
	/*
	 * With Transaction and With ATAssertion on operation level
	 */
	@Test
	public void test3DBs35() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}
	
	@Test
	public void test3DBs36() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "s=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}
	
	@Test
	public void test3DBs37() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	public void test3DBs38() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "s=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appName, wsatURL, serverRollbackResult, "0");
	}
	
	/*
	 * Without Transaction and With ATAssertion on operation level
	 */
	@Test
	public void test3DBs39() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, noTrans, "1", "0", "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs40() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "s=" + commit + ":" + basicURL + ":"
				+ server2Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, noTrans, "1", "0", "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs41() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ rollback + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, noTrans, "1", "0", "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs42() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "s=" + rollback + ":" + basicURL + ":"
				+ server2Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, noTrans, "1", "0", "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs58() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + rollback + ":" + basicURL + ":"
				+ server2Port + "&" + clientName + "=" + "commitincatch";
		commonTest(appName, wsatURL, serverRollbackResult, "1", "1", "0");
	}
}
