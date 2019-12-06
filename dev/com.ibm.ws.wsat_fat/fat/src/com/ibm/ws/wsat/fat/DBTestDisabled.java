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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServerFactory;

/**
 * ========== Test Case Readme ==========
 * More detail information, please take a look the index page of the wsatApp
 * There are 2 servers: client, server1
 * 
 * testDBDisabled01_WOTWOAO_Commit:       Without Client Transaction,              No ATAssertion on this operation,      client commit,       server1 commit      Result: 1, 1
 * 
 * testDBDisabled02_WOTWOAO_Rollback:     Without Client Transaction,              No ATAssertion on this operation,     client commit,       server1 rollback     Result: 1, 0
 * 
 * testDBDisabled03_WOTWAO_Commit:        Without Client Transaction,                 ATAssertion on this operation,     client commit,       server1 commit       Result: 1, 0 and WS-AT Feature is not installed
 * 
 * testDBDisabled04_WOTWAO_Rollback:      Without Client Transaction,                 ATAssertion on this operation,     client commit,       server1 rollback     Result: 1, 0 and WS-AT Feature is not installed
 * 
 * 
 * 
 * testDBDisabled05_WTWOA_Commit:           With Client Transaction,               No ATAssertion on this operation,     client commit,       server1 commit       Result: 1, 1
 * 
 * testDBDisabled06_WTWOA_Rollback:         With Client Transaction,               No ATAssertion on this operation,     client rollback,     server1 commit       Result: 0, 1
 * 
 * testDBDisabled07_WTWA_Commit:            With Client Transaction,                  ATAssertion on this operation,     client commit,       server1 commit       Result: 0, 0 and WS-AT Feature is not installed
 * 
 * testDBDisabled08_WTWA_Rollback:          With Client Transaction,                   TAssertion on this operation,     client rollback,     server1 commit       Result: 0, 0 and WS-AT Feature is not installed
 * 
 * 
 * 
 * testDBDisabled09_WOTWOAOo_Commit:      Without Client Transaction,     No Optional ATAssertion on this operation,     client commit,       server1 commit       Result: 1, 1
 * 
 * testDBDisabled10_WOTWAOo_Commit:       Without Client Transaction,        Optional ATAssertion on this operation,     client commit,       server1 commit       Result: 1, 0 and WS-AT Feature is not installed
 * 
 * testDBDisabled11_WTWOAOo_Commit:          With Client Transaction,     No Optional ATAssertion on this operation,     client commit,       server1 commit       Result: 1, 1
 * 
 * testDBDisabled12_WTWAOo_Commit:           With Client Transaction,        Optional ATAssertion on this operation,     client commit,       server1 commit       Result: 0, 0 and WS-AT Feature is not installed
 * 
 * 
 * 
 * testDBDisabled13_WOTWAS_Commit:       Without Client Transaction,                    ATAssertion on this service,     client commit,       server1 commit       Result: 1, 0 and WS-AT Feature is not installed
 * 
 * testDBDisabled14_WTWAS_Commit:           With Client Transaction,                    ATAssertion on this service,     client commit,       server1 commit       Result: 0, 0 and WS-AT Feature is not installed
 * 
 * 
 * 
 * testDBDisabled15_WOTWOASo_Commit:      Without Client Transaction,          Optional ATAssertion on this service,     client commit,       server1 commit       Result: 1, 0 and WS-AT Feature is not installed
 * 
 * testDBDisabled16_WTWASo_Commit:           With Client Transaction,          Optional ATAssertion on this service,     client commit,       server1 commit       Result: 0, 0 and WS-AT Feature is not installed
 * 
 */

public class DBTestDisabled extends DBTestBase {

	public static String notInstalled = "WS-AT Feature is not installed";

	@BeforeClass
	public static void beforeTests() throws Exception {

		// Server Information
		serverRollbackResult = "Throw exception for rollback from server side!";

		// Basic URL
		client = LibertyServerFactory
				.getLibertyServer("WSATDBDisabled_Client");
		server1 = LibertyServerFactory
				.getLibertyServer("WSATDBDisabled_Server1");

		DBTestBase.initWSATTest(client);

		CLient_URL = "http://" + client.getHostname() + ":"
				+ client.getHttpDefaultPort();
		Server1_URL = "http://" + server1.getHostname() + ":"
				+ server1Port;

		// Test URL
		// ATAssertion exists on Operation level in WSDL
		appName = "wsatApp";
		
		// ATAssertion with Optional=true exists on Operation level in WSDL
		appNameOptional = "wsatAppOptional";
		
		// ATAssertion exists on Service level in WSDL
		appNameService = "wsatAppService";
		
		// ATAssertion with Optional=true exists on Service level in WSDL
		appNameServiceOptional = "wsatAppServiceOptional";

		if (client != null && !client.isStarted()) {
			client.startServer();
		}
		if (server1 != null && !server1.isStarted()) {
			server1.startServer();
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		ServerUtils.stopServer(client);
		ServerUtils.stopServer(server1);

		DBTestBase.cleanupWSATTest(client);
	}

	@Test
	public void testDBDisabled01() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testDBDisabled02() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ rollback + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, serverRollbackResult, "1", "0");
	}

	@Test
	public void testDBDisabled03() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, notInstalled, "1", "0");
	}

	@Test
	public void testDBDisabled04() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ rollback + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, notInstalled, "1", "0");
	}

	@Test
	public void testDBDisabled05() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testDBDisabled06() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port + "&"
				+ clientName + "=" + rollback;
		commonTest(appName, wsatURL, goodResult, "0", "1");
	}

	@Test
	public void testDBDisabled07() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port;
		commonTest(appName, wsatURL, notInstalled, "0");
	}

	@Test
	public void testDBDisabled08() {
		String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&" + clientName + "=" + rollback;
		commonTest(appName, wsatURL, notInstalled, "0");
	}
	
	@Test
	public void testDBDisabled09() {
		String testURL = "/" + appNameOptional + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testDBDisabled10() {
		String testURL = "/" + appNameOptional + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, goodResult, "1");
	}
	
	@Test
	public void testDBDisabled11() {
		String testURL = "/" + appNameOptional + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "s="
				+ commit + ":" + basicURL + ":" + server1Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testDBDisabled12() {
		String testURL = "/" + appNameOptional + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}
	
	@Test
	public void testDBDisabled13() {
		String testURL = "/" + appNameService + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, notInstalled, "1", "0");
	}

	@Test
	public void testDBDisabled14() {
		String testURL = "/" + appNameService + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port;
		commonTest(appName, wsatURL, notInstalled, "0");
	}
	
	@Test
	public void testDBDisabled15() {
		String testURL = "/" + appNameServiceOptional + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port
				+ "&withouttrans=true";
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testDBDisabled16() {
		String testURL = "/" + appNameServiceOptional + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name + "p="
				+ commit + ":" + basicURL + ":" + server1Port;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Override
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
		
		try {
			String initValue = "0";
			// Clean DB count to zero first
			InitDB(clientInitURL, clientName, initValue);
			InitDB(server1InitURL, server1Name, initValue);

			String result = executeWSAT(testURL);
			assertTrue("Check result, expect is " + expectResult
					+ ", result is " + result, expectResult.equals(result));

			// Check WS-AT result
			if (expValule.length == 1) {
				CheckDB(clientResultURL, clientName, expValule[0]);
				CheckDB(server1ResultURL, server1Name, expValule[0]);
			}
			else if (expValule.length == 2) {
				CheckDB(clientResultURL, clientName, expValule[0]);
				CheckDB(server1ResultURL, server1Name, expValule[1]);
			} else {
				fail("Exception happens: Wrong expect value number: " + expValule.length);
			}
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}
