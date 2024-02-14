/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

public class MultiRecoveryTest {
	protected static String BASE_URL;
	protected static String BASE_URL2;

	protected final static int REQUEST_TIMEOUT = HttpUtils.DEFAULT_TIMEOUT;

	private final static String recoveryClient = "recoveryClient";
	protected final static String recoveryServer = "recoveryServer";

	protected static final String str = "Performed recovery for ";
	protected static final String failMsg = " did not perform recovery for ";

	@Server("WSATRecoveryClient1")
	public static LibertyServer server1;

	@Server("WSATRecoveryServer1")
	public static LibertyServer server2;

    protected static SetupRunner runner;
    protected static WebArchive clientApp;
    protected static WebArchive serverApp;

    @BeforeClass
	public static void beforeClass() throws Exception {
		Log.info(MultiRecoveryTest.class, "beforeClass", "");

		//		System.getProperties().entrySet().stream().forEach(e -> Log.info(MultiRecoveryTest.class, "beforeTests", e.getKey() + " -> " + e.getValue()));

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
	}
	
	@Before
	public void before() throws Exception {
		Log.info(MultiRecoveryTest.class, "before", "");
		FATUtils.startServers(runner, server1, server2);

		WSATTest.callClearResourcesServlet(recoveryServer, server1, server2);
	}

	@After
	public void after() throws Exception {
		Log.info(MultiRecoveryTest.class, "after", "");
		FATUtils.stopServers(server1, server2);
	}

	protected void recoveryTest(LibertyServer server, LibertyServer server2, String id, String crashingServers) throws Exception {
		recoveryTest(server, server2, id, crashingServers, crashingServers);
	}
	
	protected void recoveryTest(LibertyServer server, LibertyServer server2, String id, String crashingServers, String restartingServers) throws Exception {
		final String method = "recoveryTest";
		String result = null;

		try {
			// We expect this to fail since it is gonna crash the server
			callSetupServlet(id);
		} catch (IOException e) {
			// This is fine. The setup servlet crashed its server
		}

		// wait for crashing servers to have gone away
		if ("both".equals(crashingServers) || crashingServers.equals("server1")) {
			assertNotNull(server.getServerName() + " did not crash", server.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
			server.resetStarted();
			server.postStopServerArchive();
		}
		if ("both".equals(crashingServers) || crashingServers.equals("server2")) {
			assertNotNull(server2.getServerName() + " did not crash", server2.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
			server2.resetStarted();
			server2.postStopServerArchive();
		}

		// Start the ones that need restarting
		if (!"none".equals(restartingServers)) {
			if ("both".equals(restartingServers) || restartingServers.equals("server1")) {
				FATUtils.startServers(runner, server);
			}
			if ("both".equals(restartingServers) || restartingServers.equals("server2")) {
				FATUtils.startServers(runner, server2);
			}
		}

		// Wait for the relevant server to have done its stuff
		if (crashingServers.equals(restartingServers)) {
			// This is traditional recovery and it might have already happened so clear the log marks
			if ("both".equals(crashingServers) || crashingServers.equals("server1")) {
				server.clearLogMarks();
				assertNotNull(server.getServerName()+failMsg+server.getServerName(), server.waitForStringInTrace(str+server.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
			}
			if ("both".equals(crashingServers) || crashingServers.equals("server2")) {
				server2.clearLogMarks();
				assertNotNull(server2.getServerName()+failMsg+server2.getServerName(), server2.waitForStringInTrace(str+server2.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
			}

			try {
				result = callCheckServlet(id);
			} catch (Exception e) {
				Log.error(getClass(), method, e);
				throw e;
			}
		} else {
			// This is peer recovery
			checkPeerRecovery(server, server2, crashingServers, restartingServers, id);
		}

		Log.info(getClass(), method, "callCheckServlet(" + id + ") returned: " + result);
	}

	protected void checkPeerRecovery(LibertyServer server, LibertyServer server2, String crashingServers, String restartingServers, String testNumber) throws IOException {}

	protected String callCheckServlet(String testNumber) throws IOException {
		final String method = "callCheckServlet";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "MultiRecoveryCheckServlet";
		//check XAResource status on server1
		String urlStr1 = BASE_URL + "/" + recoveryServer + "/" + servletName
				+ "?number=" + testNumber+"01";
		//check XAResource status on server2
		String urlStr2 = BASE_URL2 + "/" + recoveryServer + "/" + servletName
				+ "?number=" + testNumber+"02";

		String result1 = "";
		String result2 = "";

		URL url1 = new URL(urlStr1);
		URL url2 = new URL(urlStr2);

		while (result1.isEmpty() || result2.isEmpty()) {
			if (result1.isEmpty()) {
				HttpURLConnection con1 = null;

				try {
					Log.info(getClass(), method, "Getting connection to " + urlStr1);
					con1 = HttpUtils.getHttpConnection(url1, expectedConnectionCode, REQUEST_TIMEOUT);
					if (con1 != null) {
						Log.info(getClass(), method, "Getting result from " + urlStr1);
						result1 = HttpUtils.getConnectionStream(con1).readLine();
						Log.info(getClass(), method, "result: " + result1);
					}
				} catch (Exception e) {
					Log.error(getClass(), method, e);
				} finally {
					if (con1 != null) {
						con1.disconnect();
					}
				}
			}

			if (result2.isEmpty()) {
				HttpURLConnection con2 = null;

				try {
					Log.info(getClass(), method, "Getting connection to " + urlStr2);
					con2 = HttpUtils.getHttpConnection(url2, expectedConnectionCode, REQUEST_TIMEOUT);
					if (con2 != null) {
						Log.info(getClass(), method, "Getting result from " + urlStr2);
						result2 = HttpUtils.getConnectionStream(con2).readLine();
						Log.info(getClass(), method, "result: " + result2);
					}
				} catch (Exception e) {
					Log.error(getClass(), method, e);
				} finally {
					if (con2 != null) {
						con2.disconnect();
					}
				}
			}

			// Do we need to retry?
			if (result1.isEmpty() || result2.isEmpty()) {
				Log.info(getClass(), method, "Sleeping 5 seconds before retrying");
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					Log.error(getClass(), method, e);
				}
			}
		}

		Log.info(getClass(), method, "Recovery test " + testNumber + 
				"\n Result1 : " + result1 + 
				"\n Result2 : " + result2);

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

		return	"\n Result1 : " + result1 + 
				"\n Result2 : " + result2;
	}

	protected void callSetupServlet(String testNumber) throws IOException{
		final String method = "callSetupServlet";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "MultiRecoverySetupServlet";
		int test_number = Integer.parseInt(testNumber);
		if (test_number == 1)
			expectedConnectionCode = HttpURLConnection.HTTP_NOT_FOUND;

		String urlStr = BASE_URL + "/" + recoveryClient + "/" + servletName
				+ "?number=" + testNumber + "&baseurl=" + BASE_URL
				+ "&baseurl2=" + BASE_URL2;
	
		Log.info(getClass(), method, "callSetupServlet URL: " + urlStr);
		String result = "";
		HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try {
			BufferedReader br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
		} finally {
			con.disconnect();
		}
		assertNotNull(result);
		Log.info(getClass(), method, "Recovery test " + testNumber + " Result : " + result);
	}

	protected void assert1stArgRecoveredFor2ndArg(LibertyServer recoverer, LibertyServer recoveree) {
		assertNotNull(recoverer.getServerName()+failMsg+recoveree.getServerName(), recoverer.waitForStringInTrace(str+recoveree.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
	}
}
