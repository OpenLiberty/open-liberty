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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
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

public class DBServiceTest extends DBTestBase {

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
		server2 = LibertyServerFactory
				.getLibertyServer("WSATDB_Server2");

		DBTestBase.initWSATTest(client);
		DBTestBase.initWSATTest(server1);
		DBTestBase.initWSATTest(server2);

		CLient_URL = "http://" + client.getHostname() + ":"
				+ client.getHttpDefaultPort();
		Server1_URL = "http://" + server1.getHostname() + ":"
				+ server1Port;
		Server2_URL = "http://" + server2.getHostname() + ":"
				+ server2Port;

		// Test URL
		// ATAssertion exists on Operation level in WSDL
		appNameService = "wsatAppService";

		ShrinkHelper.defaultDropinApp(client, appNameService, "com.ibm.ws."+appNameService+".client","com.ibm.ws."+appNameService+".server","com.ibm.ws."+appNameService+".servlet","com.ibm.ws."+appNameService+".utils");
		ShrinkHelper.defaultDropinApp(server1, appNameService, "com.ibm.ws."+appNameService+".client","com.ibm.ws."+appNameService+".server","com.ibm.ws."+appNameService+".servlet","com.ibm.ws."+appNameService+".utils");
		ShrinkHelper.defaultDropinApp(server2, appNameService, "com.ibm.ws."+appNameService+".client","com.ibm.ws."+appNameService+".server","com.ibm.ws."+appNameService+".servlet","com.ibm.ws."+appNameService+".utils");

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
	
	/*
	 * With/Without Transaction and With ATAssertion on service level
	 */
	@Test
	public void test3DBs51() {
		String testURL = "/" + appNameService + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appNameService, wsatURL, goodResult, "1");
	}
	
	@Test
	public void test3DBs52() {
		String testURL = "/" + appNameService + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "s=" + rollback + ":" + basicURL + ":"
				+ server2Port;
		commonTest(appNameService, wsatURL, serverRollbackResult, "0");
	}
	
	@Test
	@Mode(TestMode.FULL)
	public void test3DBs53() {
		String testURL = "/" + appNameService + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ server2Name + "p=" + commit + ":" + basicURL + ":"
				+ server2Port
				+ "&withouttrans=true";
		commonTest(appNameService, wsatURL, noTrans, "1", "0", "0");
	}
}
