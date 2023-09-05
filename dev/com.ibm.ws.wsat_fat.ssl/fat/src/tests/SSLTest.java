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
package tests;

import java.util.Hashtable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
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

		FATUtils.startServers(client, server1, server2);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(client, server1, server2);

		DBTestBase.cleanupWSATTest(client);
		DBTestBase.cleanupWSATTest(server1);
		DBTestBase.cleanupWSATTest(server2);
	}

	static ServerConfiguration clientAuthentify(LibertyServer server) throws Exception {
        ServerConfiguration originalConfig = null;
		try {
			ServerConfiguration config = server.getServerConfiguration();
			originalConfig = config.clone();
			SSL ssl = config.getSSLById("myDefaultSSLConfig");
			ssl.setClientAuthentication(true);
			ssl.setClientAuthenticationSupported(true);
			server.setMarkToEndOfLog();
			server.setConfigUpdateTimeout(180000);
			server.updateServerConfiguration(config);
			server.waitForConfigUpdateInLogUsingMark(null, false);
		} catch (Exception e) {
            try {
                server.updateServerConfiguration(originalConfig);
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            throw e;
		}
        
        return originalConfig;
	}

	private static AutoCloseable clientAuthentify() throws Exception {
		Hashtable<LibertyServer, ServerConfiguration> originalConfigs = new Hashtable<LibertyServer, ServerConfiguration>();

		originalConfigs.put(client,  clientAuthentify(client));
		originalConfigs.put(server1,  clientAuthentify(server1));
		originalConfigs.put(server2,  clientAuthentify(server2));

        return () -> {
        	for (LibertyServer server : originalConfigs.keySet()) {
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(originalConfigs.get(server));
                server.waitForConfigUpdateInLogUsingMark(null, true);
        	}
        };
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
		try (AutoCloseable x = clientAuthentify()) {
			final String testURL = "/" + appName + "/ClientServlet";
			String wsatURL = CLient_URL + testURL + "?" + server1Name
					+ "p=" + commit + ":" + basicURL + ":"
					+ server1Port + "&" + server2Name + "p="
					+ commit + ":" + basicURL + ":"
					+ server2Port;
			commonTest(appName, wsatURL, goodResult, "1");
		}
	}

	@Test
	public void testSSL_ClientRollbackByProxy_WithClientAuth() throws Exception {
		try (AutoCloseable x = clientAuthentify()) {
			final String testURL = "/" + appName + "/ClientServlet";
			String wsatURL = CLient_URL + testURL + "?" + server1Name
					+ "p=" + commit + ":" + basicURL + ":"
					+ server1Port + "&" + server2Name + "p="
					+ commit + ":" + basicURL + ":"
					+ server2Port + "&" + clientName + "="
					+ rollback;
			commonTest(appName, wsatURL, goodResult, "0");
		}
	}

	@Test
	public void testSSL_Server2RollbackByProxy_WithClientAuth() throws Exception {
		try (AutoCloseable x = clientAuthentify()) {
			final String testURL = "/" + appName + "/ClientServlet";
			String wsatURL = CLient_URL + testURL + "?" + server1Name
					+ "p=" + commit + ":" + basicURL + ":"
					+ server1Port + "&" + server2Name + "p="
					+ rollback + ":" + basicURL + ":"
					+ server2Port;
			commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "0");
		}
	}
}