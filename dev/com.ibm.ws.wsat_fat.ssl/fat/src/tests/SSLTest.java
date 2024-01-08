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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;

import javax.net.ssl.HttpsURLConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATSecurityUtils;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class SSLTest extends DBTestBase {
	
	protected static boolean startServers = true;

	public static int server1SecurePort = 9444;
	public static int server2SecurePort = 9445;

	protected static SetupRunner runner;

	@BeforeClass
	public static void beforeTests() throws Exception {
		
		runner = new SetupRunner() {
	        @Override
	        public void run(LibertyServer s) throws Exception {
	        	Log.info(SSLTest.class, "setupRunner.run", "Setting up "+s.getServerName());
	        }
	    };
	    
	    HttpUtils.trustAllCertificates();;

		// Test URL
		appName = "wsatApp";
		basicURL = "https://localhost";
		
		client = LibertyServerFactory
				.getLibertyServer("WSATSSL_Client");
		server1 = LibertyServerFactory
				.getLibertyServer("WSATSSL_Server1");
		server1.setHttpDefaultPort(server1Port);
		server1.setHttpDefaultSecurePort(server1SecurePort);
		server2 = LibertyServerFactory
				.getLibertyServer("WSATSSL_Server2");
		server2.setHttpDefaultPort(server2Port);
		server2.setHttpDefaultSecurePort(server2SecurePort);

		DBTestBase.initWSATTest(client);
		DBTestBase.initWSATTest(server1);
		DBTestBase.initWSATTest(server2);

		ShrinkHelper.defaultDropinApp(client, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");
		ShrinkHelper.defaultDropinApp(server1, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");
		ShrinkHelper.defaultDropinApp(server2, appName, "com.ibm.ws."+appName+".client","com.ibm.ws."+appName+".server","com.ibm.ws."+appName+".servlet","com.ibm.ws."+appName+".utils");

		CLient_URL = "https://" + client.getHostname() + ":"
				+ client.getHttpDefaultSecurePort();
		Server1_URL = "https://" + server1.getHostname() + ":"
				+ server1.getHttpDefaultSecurePort();
		Server2_URL = "https://" + server2.getHostname() + ":"
				+ server2.getHttpDefaultSecurePort();
				
		// Create keys
		FATUtils.startServers(runner, client, server1, server2);
		FATUtils.stopServers(client, server1, server2);

//		FATSecurityUtils.createKeys(client, server1, server2);
		FATSecurityUtils.extractPublicCertifcate(client, server1, server2);
		FATSecurityUtils.establishTrust(client, server1);
		FATSecurityUtils.establishTrust(client, server2);
		FATSecurityUtils.establishTrust(server1, client);
		FATSecurityUtils.establishTrust(server1, server2);
		FATSecurityUtils.establishTrust(server2, client);
		FATSecurityUtils.establishTrust(server2, server1);
		
		// Start servers unless we're going to do it in a subclass
		if (startServers) {
			FATUtils.startServers(runner, client, server1, server2);
		} else {
			Log.info(SSLTest.class, "beforeTests", "Not starting servers here");
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(client, server1, server2);

		DBTestBase.cleanupWSATTest(client);
		DBTestBase.cleanupWSATTest(server1);
		DBTestBase.cleanupWSATTest(server2);
	}

	@Override
	public void InitDB(String url, String serverName, String value)
			throws Exception {
		String method = "InitDB";
		Log.info(getClass(), method, "Init " + serverName + " DB from " + url + ": "
				+ value);
		BufferedReader br = null;
		try {
			HttpsURLConnection con = (HttpsURLConnection)getHttpConnection(new URL(url),
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			con.setReadTimeout(30000);
			br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			Log.info(getClass(), method, "Init " + serverName + " DB from " + url + ": "
					+ result);
			assertTrue("Init " + serverName + " DB from "+url+", expect is 0, result is "
					+ result, result.equals(value));
		} finally {
			if (br != null)
				br.close();
		}
	}

	static ServerConfiguration clientAuthentify(LibertyServer server) throws Exception {
        ServerConfiguration originalConfig = null;
		try {
			ServerConfiguration config = server.getServerConfiguration();
			originalConfig = config.clone();
			for (SSL ssl : config.getSsls()) {
				Log.info(SSLTest.class, "clientAuthentify", "Adding client auth to "+ssl.getId()+" for "+server.getServerName());
				ssl.setClientAuthentication(true);
				ssl.setClientAuthenticationSupported(true);
				server.setMarkToEndOfLog();
				server.setConfigUpdateTimeout(180000);
				server.updateServerConfiguration(config);
				server.waitForConfigUpdateInLogUsingMark(null, false);
				break; /* There should be only one */
			}
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
        		Log.info(SSLTest.class, "clientAuthentify", "Restoring config for "+server.getServerName());
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(originalConfigs.get(server));
                server.waitForConfigUpdateInLogUsingMark(null, true);
        	}
        };
    }

	@Test
	public void testSSL_AllCommitByProxy() {
		client.waitForStringInLog("CWLIB0206I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1SecurePort + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2SecurePort;
		commonTest(appName, wsatURL, goodResult, "1");
	}

	@Test
	public void testSSL_ClientRollbackByProxy() {
		server1.waitForStringInLog("CWLIB0206I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1SecurePort + "&" + server2Name + "p="
				+ commit + ":" + basicURL + ":"
				+ server2SecurePort + "&" + clientName + "="
				+ rollback;
		commonTest(appName, wsatURL, goodResult, "0");
	}
	
	@Test
	public void testSSL_Server2RollbackByProxy() {
		server2.waitForStringInLog("CWLIB0206I");
		final String testURL = "/" + appName + "/ClientServlet";
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "p=" + commit + ":" + basicURL + ":"
				+ server1SecurePort + "&" + server2Name + "p="
				+ rollback + ":" + basicURL + ":"
				+ server2SecurePort;
		commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "0");
	}

	@Test
	public void testSSL_AllCommitByProx_WithClientAuth() throws Exception {
		try (AutoCloseable x = clientAuthentify()) {
			final String testURL = "/" + appName + "/ClientServlet";
			String wsatURL = CLient_URL + testURL + "?" + server1Name
					+ "p=" + commit + ":" + basicURL + ":"
					+ server1SecurePort + "&" + server2Name + "p="
					+ commit + ":" + basicURL + ":"
					+ server2SecurePort;
			commonTest(appName, wsatURL, goodResult, "1");
		}
	}

	@Test
	public void testSSL_ClientRollbackByProxy_WithClientAuth() throws Exception {
		try (AutoCloseable x = clientAuthentify()) {
			final String testURL = "/" + appName + "/ClientServlet";
			String wsatURL = CLient_URL + testURL + "?" + server1Name
					+ "p=" + commit + ":" + basicURL + ":"
					+ server1SecurePort + "&" + server2Name + "p="
					+ commit + ":" + basicURL + ":"
					+ server2SecurePort + "&" + clientName + "="
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
					+ server1SecurePort + "&" + server2Name + "p="
					+ rollback + ":" + basicURL + ":"
					+ server2SecurePort;
			commonTest(appName, wsatURL, "Throw exception for rollback from server side!", "0");
		}
	}
}