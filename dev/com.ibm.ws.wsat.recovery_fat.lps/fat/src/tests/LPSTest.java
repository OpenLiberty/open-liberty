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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class LPSTest {
	@Server("WSATRecoveryClient1")
	public static LibertyServer server1;
	private static String BASE_URL;

	@Server("WSATRecoveryServer1")
	public static LibertyServer server2;
	private static String BASE_URL2;

	private final static int REQUEST_TIMEOUT = 10;

	@BeforeClass
	public static void beforeTests() throws Exception {
		BASE_URL = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort();
		server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

        final WebArchive recoveryClient = ShrinkHelper.buildDefaultApp("recoveryClient", "client.*");
		ShrinkHelper.exportDropinAppToServer(server1, recoveryClient);
		ShrinkHelper.exportDropinAppToServer(server2, recoveryClient);

        final WebArchive recoveryServer = ShrinkHelper.buildDefaultApp("recoveryServer", "server.*");
		ShrinkHelper.exportDropinAppToServer(server1, recoveryServer);
		ShrinkHelper.exportDropinAppToServer(server2, recoveryServer);

		server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
		server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
	}

	@Before
	public void beforeTest() throws Exception {
		WSATTest.deleteStateFiles(server1, server2);
		FATUtils.startServers(server1, server2);
	}
	
	@After
    public void tearDown() throws Exception {
		FATUtils.stopServers(new String[] {"WTRN0046E", "WTRN0048W", "WTRN0049W", "WTRN0094W"}, server1, server2);
    }
	
	/**
	 * 
	 * Multiple Recovery + LPS test
	 */
	
	@Test
	public void WSTXLPS301AFVT() throws Exception {
		recoveryTest("3011","server1");
	}
	
	@Test
	@ExpectedFFDC("javax.transaction.xa.XAException")
	public void WSTXLPS301BFVT() throws Exception {
		recoveryTest("3012","server2");
		if (server1 != null && server1.isStarted()) {
			server1.stopServer("WTRN0049W"); //ensure server has stopped
		}
	}
	
	@Test
	public void WSTXLPS301CFVT() throws Exception {
		recoveryTest("3013","both");
	}
	
	@Test
	public void WSTXLPS302AFVT() throws Exception {
		recoveryTest("3021","server1");
	}
	
	@Test
	@ExpectedFFDC("javax.transaction.xa.XAException")
	public void WSTXLPS302BFVT() throws Exception {
		recoveryTest("3022","server2");
		if (server1 != null && server1.isStarted()) {
			server1.stopServer("WTRN0049W"); //ensure server has stopped
		}
	}
	
	@Test
	public void WSTXLPS302CFVT() throws Exception {
		recoveryTest("3023","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"})
	public void WSTXLPS303AFVT() throws Exception {
		recoveryTest("3031","server1");
	}
	
	protected void recoveryTest(String id, String startServer) throws Exception {
        final String method = "recoveryTest";
        String result = null;

        try {
            // We expect this to fail since it is gonna crash the server
        	result = callSetupServlet(id);
        } catch (Throwable e) {
        	// This is fine
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + result);
        
        //restart server in three modes
        if (startServer.equals("server1")){
        		restartServer(server1);
                server1.waitForStringInTrace("Performed recovery for "+server1.getServerName());
        } else if (startServer.equals("server2")){
        		restartServer(server2);
                server2.waitForStringInTrace("Performed recovery for "+server2.getServerName());
        } else if (startServer.equals("both")){
        		restartServer(server1);
            	restartServer(server2);
                server1.waitForStringInTrace("Performed recovery for "+server1.getServerName());
                server2.waitForStringInTrace("Performed recovery for "+server2.getServerName());
        }

        callCheckServlet(id);
    }

	private String callSetupServlet(String testNumber) throws IOException{
		String method = "callSetupServlet";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "MultiRecoverySetupServlet";
		int test_number = Integer.parseInt(testNumber);
		if (test_number == 1)
			expectedConnectionCode = HttpURLConnection.HTTP_NOT_FOUND;
		String urlStr = BASE_URL + "/recoveryClient/" + servletName
				+ "?number=" + testNumber + "&baseurl=" + BASE_URL
				+ "&baseurl2=" + BASE_URL2;

		Log.info(getClass(), method, "callSetupServlet URL: " + urlStr);
		String result = "";
		HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try{
			BufferedReader br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
		}finally{
			con.disconnect();
		}
		assertNotNull(result);
		Log.info(getClass(), method, "Recover test " + testNumber + " Result : " + result);
		assertTrue("Cannot get expected reply from server",
				!result.contains("failed"));
		return "";
	}

	private String callCheckServlet(String testNumber) throws IOException {
		final String method = "callCheckServlet";
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "MultiRecoveryCheckServlet";
		//check XAResource status on server1
		String urlStr1 = BASE_URL + "/recoveryServer/" + servletName
				+ "?number=" + testNumber+"01";
		//check XAResource status on server2
		String urlStr2 = BASE_URL2 + "/recoveryServer/" + servletName
				+ "?number=" + testNumber+"02";
		Log.info(getClass(), method, "callCheckServlet URL1: " + urlStr1);
		Log.info(getClass(), method, "callCheckServlet URL2: " + urlStr2);
		String result1 = "";
		HttpURLConnection con1 = HttpUtils.getHttpConnection(new URL(urlStr1), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try {
	        BufferedReader br1 = HttpUtils.getConnectionStream(con1);
	        result1 = br1.readLine();
		} finally {
			con1.disconnect();
		}
		String result2 = "";
		HttpURLConnection con2 = HttpUtils.getHttpConnection(new URL(urlStr2), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try {
	        BufferedReader br2 = HttpUtils.getConnectionStream(con2);
	        result2 = br2.readLine();
		} finally {
			con2.disconnect();
		}
        assertNotNull(result1);
        assertNotNull(result2);
        Log.info(getClass(), method, "Recover test " + testNumber + 
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
	
	private static void restartServer(LibertyServer s) throws Exception{
        // wait for 1st server to have gone away
        assertNotNull(s.getServerName() + " did not crash", s.waitForStringInTrace(XAResourceImpl.DUMP_STATE));
        s.resetStarted();
		FATUtils.startServers(s);
		s.resetLogMarks();
	}
}
