/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@AllowedFFDC(value = { "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException", "java.io.FileNotFoundException", "java.net.SocketException" })
@RunWith(FATRunner.class)
public class ReroutePeerRecoveryTest extends MultiRecoveryTest {

	/*
	 * server1, WSATRecoveryClient1 (8010), is the primary client
	 * server2, WSATRecoveryServer1 (8030), is the primary server (because history)
	 * server3, WSATRecoveryClient2 (8070), is the secondary client which will peer recover the primary client
	 * server4, WSATRecoveryClient3 (8050), is for re-routing. Receives messages meant for another client
	 * server5, WSATRecoveryServer2 (8090), is the secondary server which will peer recover the primary server
	 * server6, WSATRecoveryServer3 (9010), is for re-routing. Receives messages meant for another server
	 */
	@Server("WSATRecoveryClient2")
	public static LibertyServer server3;

	@Server("WSATRecoveryClient3")
	public static LibertyServer server4;

	@Server("WSATRecoveryServer2")
	public static LibertyServer server5;

	@Server("WSATRecoveryServer3")
	public static LibertyServer server6;
	
	public static LibertyServer[] serversToStart;
	public static LibertyServer[] serversToStop;

	@BeforeClass
	public static void beforeClass() throws Exception {
		Log.info(ReroutePeerRecoveryTest.class, "beforeClass", "");
		String method = "beforeClass";

		runner = new SetupRunner() {
	        @Override
	        public void run(LibertyServer s) throws Exception {
	        	Log.info(MultiRecoveryTest.class, "setupRunner.run", "Setting up "+s.getServerName());
	            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
	        }
	    };
	    
		BASE_URL = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort();

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

        clientApp = ShrinkHelper.buildDefaultApp("recoveryClient", "client.*");
		ShrinkHelper.exportDropinAppToServer(server1, clientApp);
		ShrinkHelper.exportDropinAppToServer(server2, clientApp);

        serverApp = ShrinkHelper.buildDefaultApp("recoveryServer", "server.*");
		ShrinkHelper.exportDropinAppToServer(server1, serverApp);
		ShrinkHelper.exportDropinAppToServer(server2, serverApp);

		int port = Integer.parseInt(System.getProperty("HTTP_quaternary"));
    	Log.info(ReroutePeerRecoveryTest.class, method, "Setting port for " + server3.getServerName() + " to " + port);
    	server3.setHttpDefaultPort(port);

    	port = Integer.parseInt(System.getProperty("HTTP_tertiary"));
    	Log.info(ReroutePeerRecoveryTest.class, method, "Setting port for " + server4.getServerName() + " to " + port);
		server4.setHttpDefaultPort(port);

    	port = Integer.parseInt(System.getProperty("HTTP_quinary"));
    	Log.info(ReroutePeerRecoveryTest.class, method, "Setting port for " + server5.getServerName() + " to " + port);
		server5.setHttpDefaultPort(port);

    	port = Integer.parseInt(System.getProperty("HTTP_senary"));
    	Log.info(ReroutePeerRecoveryTest.class, method, "Setting port for " + server6.getServerName() + " to " + port);
		server6.setHttpDefaultPort(port);

		ShrinkHelper.exportDropinAppToServer(server3, clientApp);
		ShrinkHelper.exportDropinAppToServer(server5, clientApp);
		ShrinkHelper.exportDropinAppToServer(server3, serverApp);
		ShrinkHelper.exportDropinAppToServer(server5, serverApp);
	}

	@After
	public void after() throws Exception {
		Log.info(ReroutePeerRecoveryTest.class, "after", "");
		FATUtils.stopServers(allowedMsgs, server1, server2, server3, server4, server5, server6);
	}

	@Before
	public void before() throws Exception {
		Log.info(ReroutePeerRecoveryTest.class, "before", "");
		FATUtils.startServers(runner, server1, server2, server3, server4, server5, server6);
		WSATTest.callClearResourcesServlet(recoveryServer, server3, server5);
		server3.setTraceMarkToEndOfDefaultTrace();
		server5.setTraceMarkToEndOfDefaultTrace();
	}
	
	private static final String[] allowedMsgs = new String[] {"WTRN0048W"};
	
	@AfterClass
	public static void afterClass() throws Exception {
		Log.info(ReroutePeerRecoveryTest.class, "afterClass", "");
	}

	@Override
	protected void checkPeerRecovery(LibertyServer server1, LibertyServer server2, String crashingServers, String restartingServers, String testNumber) throws IOException {
		String result1 = null;
		String result2 = null;

		if ("server1".equals(crashingServers) || "both".equals(crashingServers) && !"server1".equals(restartingServers)) {
			assert1stArgRecoveredFor2ndArg(server3, server1);
			result1 = callCheckServlet(server3, testNumber, "01");
		}
		if ("server2".equals(crashingServers) || "both".equals(crashingServers) && !"server2".equals(restartingServers)) {
			assert1stArgRecoveredFor2ndArg(server5, server2);
			result2 = callCheckServlet(server5, testNumber, "02");
		}
		
		if (result1 == null) {
			result1 = callCheckServlet(server1, testNumber, "01");
		}
		if (result2 == null) {
			result2 = callCheckServlet(server2, testNumber, "02");
		}

		if (testNumber.startsWith("13") || testNumber.startsWith("15") || testNumber.startsWith("16")) {
			assertTrue("All XAResources should rollback but do not get "
					+ "allRollback in the result.", result2.contains("allRollback"));
		} else if (testNumber.startsWith("14")) {
			assertTrue("All XAResources should commit but do not get "
					+ "allCommit in the result.", result2.contains("allCommitted"));
		} else if (testNumber.equals("3011") || testNumber.equals("3021")) {
			assertTrue("Can not get the One Phase XAResource in STARTED state", result1.contains("The One Phase XAResource is in STARTED state."));
			assertTrue("Can not get the XAResource in ROLLEDBACK state", result2.contains("allRollback"));
		} else if (testNumber.equals("3012") || testNumber.equals("3022")) {
			assertTrue("Can not get the One Phase XAResource in ROLLEDBACK state on server1", 
					result1.contains("The One Phase XAResource is in ROLLEDBACK state"));
			assertTrue("Can not get the XAResource in ROLLEDBACK state on server2", result2.contains("allRollback"));
		} else if (testNumber.equals("3013") || testNumber.equals("3023")) {
			assertTrue("Can not get the One Phase XAResource in STARTED state", result1.contains("The One Phase XAResource is in STARTED state."));
			assertTrue("Can not get the XAResource in ROLLEDBACK state on server2", result2.contains("allRollback"));
		} else if (testNumber.equals("3031")) {
			assertTrue("Can not get the One Phase XAResource in ROLLEDBACK state on server1", 
					result1.contains("The One Phase XAResource is in ROLLEDBACK state"));
			assertTrue("Can not get the XAResource in ROLLEDBACK state on server2", result2.contains("allRollback"));
		} else {
			assertTrue("Atomicity is not satisfied.",
					result1.contains("allCommitted") == result2
					.contains("allCommitted"));
			assertTrue("Atomicity is not satisfied.",
					result1.contains("allRollback") == result2
					.contains("allRollback"));
			assertTrue(
					"Atomicity is not satisfied.",
					!result1.contains("Unatomic") && !result2.contains("Unatomic"));
		}
	}

	protected String callCheckServlet(LibertyServer server, String testNumber, String serverNumber) throws IOException {
		final String method = "callCheckServlet";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "MultiRecoveryCheckServlet";

		String result = "";

		URL url = new URL("http", server.getHostname(), server.getHttpDefaultPort(), "/" + recoveryServer + "/" + servletName
				+ "?number=" + testNumber+serverNumber);

		while (result.isEmpty()) {
			HttpURLConnection con = null;

			Log.info(getClass(), method, "Getting connection to " + url.toString());
			try {
				con = HttpUtils.getHttpConnection(url, expectedConnectionCode, REQUEST_TIMEOUT);
				if (con != null) {
					Log.info(getClass(), method, "Getting result from " + url.toString());
					result = HttpUtils.getConnectionStream(con).readLine();
				}
			} catch (Exception e) {
				Log.error(getClass(), method, e);
			} finally {
				if (con != null) {
					con.disconnect();
				}
			}

			// Do we need to retry?
			if (result.isEmpty()) {
				Log.info(getClass(), method, "Sleeping 5 seconds before retrying");
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					Log.error(getClass(), method, e);
				}
			}
		}

		Log.info(getClass(), method, "Result: " + result);
		return result;
	}

	@Test
	public void WSTXMPR008DFVT() throws Exception {
		serversToStop = new LibertyServer[]{server3,};
		serversToStart = new LibertyServer[]{server1, server3};
		recoveryTest(server1, server2, "801", "server1", "none");
	}

	@Test
	public void WSTXMPR008EFVT() throws Exception {
		serversToStop = new LibertyServer[]{server5,};
		serversToStart = new LibertyServer[]{server2, server5};
		recoveryTest(server1, server2, "802", "server2", "none");
	}

	@Test
	public void WSTXMPR009DFVT() throws Exception {
		serversToStop = new LibertyServer[]{server3,};
		serversToStart = new LibertyServer[]{server1, server3};
		recoveryTest(server1, server2, "901", "server1", "none");
	}

	@Test
	public void WSTXMPR009EFVT() throws Exception {
		serversToStop = new LibertyServer[]{server5,};
		serversToStart = new LibertyServer[]{server2, server5};
		recoveryTest(server1, server2, "902", "server2", "none");
	}
}