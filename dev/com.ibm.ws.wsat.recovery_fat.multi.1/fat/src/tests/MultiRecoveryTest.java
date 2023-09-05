/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

public class MultiRecoveryTest {
	protected static String BASE_URL;
	protected static String BASE_URL2;

	private final static int REQUEST_TIMEOUT = HttpUtils.DEFAULT_TIMEOUT;

	private final static String recoveryClient = "recoveryClient";
	private final static String recoveryServer = "recoveryServer";

	@Server("WSATRecovery1")
	public static LibertyServer server1;

	@Server("WSATRecovery2")
	public static LibertyServer server2;

	@BeforeClass
	public static void beforeTests() throws Exception {
		//		System.getProperties().entrySet().stream().forEach(e -> Log.info(MultiRecoveryTest.class, "beforeTests", e.getKey() + " -> " + e.getValue()));

		BASE_URL = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort();

		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

        final WebArchive clientApp = ShrinkHelper.buildDefaultApp("recoveryClient", "client.*");
		ShrinkHelper.exportDropinAppToServer(server1, clientApp);
		ShrinkHelper.exportDropinAppToServer(server2, clientApp);

        final WebArchive serverApp = ShrinkHelper.buildDefaultApp("recoveryServer", "server.*");
		ShrinkHelper.exportDropinAppToServer(server1, serverApp);
		ShrinkHelper.exportDropinAppToServer(server2, serverApp);

		server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
		server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);

		FATUtils.startServers(server1, server2);
	}
	
	@Before
	public void before() throws IOException {
		WSATTest.callClearResourcesServlet(recoveryServer, server1, server2);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server1, server2);
	}

	protected void recoveryTest(LibertyServer server, LibertyServer server2, String id, String startServer) throws Exception {
		final String method = "recoveryTest";
		String result = null;

		try {
			// We expect this to fail since it is gonna crash the server
			callSetupServlet(id);
		} catch (IOException e) {
			// This is fine. The setup servlet crashed its server
		}

		final String str = "Performed recovery for ";
		final String failMsg = " did not perform recovery";
		//restart server in three modes
		if (startServer.equals("server1")) {
			// wait for 1st server to have gone away
			assertNotNull(server.getServerName() + " did not crash", server.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
			FATUtils.startServers(server);
			assertNotNull(server.getServerName()+failMsg, server.waitForStringInTrace(str+server.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
		} else if (startServer.equals("server2")) {
			// wait for 2nd server to have gone away
			assertNotNull(server2.getServerName() + " did not crash", server2.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
			FATUtils.startServers(server2);
			assertNotNull(server2.getServerName()+failMsg, server2.waitForStringInTrace(str+server2.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
		} else if(startServer.equals("both")) {
			// wait for both servers to have gone away
			assertNotNull(server.getServerName() + " did not crash", server.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
			assertNotNull(server2.getServerName() + " did not crash", server2.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
			FATUtils.startServers(server, server2);
			assertNotNull(server.getServerName()+failMsg, server.waitForStringInTrace(str+server.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
			assertNotNull(server2.getServerName()+failMsg, server2.waitForStringInTrace(str+server2.getServerName(), FATUtils.LOG_SEARCH_TIMEOUT));
		}

		try {
			result = callCheckServlet(id);
		} catch (Exception e) {
			Log.error(getClass(), method, e);
			throw e;
		}
		
		Log.info(getClass(), method, "callCheckServlet(" + id + ") returned: " + result);
	}

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
		assertTrue("Cannot get expected reply from server",
				!result.contains("failed"));
	}
}
