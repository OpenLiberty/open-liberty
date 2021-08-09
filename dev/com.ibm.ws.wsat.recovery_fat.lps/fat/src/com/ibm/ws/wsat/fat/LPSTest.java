/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@AllowedFFDC(value = { "javax.transaction.SystemException", "javax.transaction.xa.XAException" })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class LPSTest {
	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;

	private final static int REQUEST_TIMEOUT = 10;
	private final static int START_TIMEOUT = 300000; // in ms

	@BeforeClass
	public static void beforeTests() throws Exception {
		server = LibertyServerFactory.getLibertyServer("WSATRecovery1");
		BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
		server2 = LibertyServerFactory.getLibertyServer("WSATRecovery2");
		server2.setHttpDefaultPort(9992);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		if (server != null && server.isStarted()){
			server.stopServer(true, true);
		}

		if (server2 != null && server2.isStarted()){
			server2.stopServer(true, true);
		}

		ShrinkHelper.defaultDropinApp(server, "recoveryClient", "com.ibm.ws.wsat.fat.client.recovery.*");
		ShrinkHelper.defaultDropinApp(server, "recoveryServer", "com.ibm.ws.wsat.fat.server.*");
		ShrinkHelper.defaultDropinApp(server2, "recoveryClient", "com.ibm.ws.wsat.fat.client.recovery.*");
		ShrinkHelper.defaultDropinApp(server2, "recoveryServer", "com.ibm.ws.wsat.fat.server.*");

		server.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);
	}
	
	@AfterClass
	public static void tearDownAll() throws Exception {
		if (server != null && server.isStarted()) {
			server.stopServer(true, true, ".*"); // ensure server has stopped
		}
		if (server2 != null && server2.isStarted()) {
			server2.stopServer(true, true, ".*"); // ensure server has stopped
		}
	}

	@Before
	public void beforeTest() throws Exception {
        if (server != null && !server.isStarted()){
            server.startServerAndValidate(false,false,true);
		}
		
		if (server2 != null && !server2.isStarted()){
			server2.startServerAndValidate(false,false,true);
		}
	}
	
	@After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(true, true, "WTRN0046E", "WTRN0048W", "WTRN0049W", "WTRN0094W"); //ensure server has stopped
        }
        
        if (server2 != null && server2.isStarted()) {
            server2.stopServer(true, true, "WTRN0046E", "WTRN0048W", "WTRN0049W", "WTRN0094W"); //ensure server has stopped
        }
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
	@AllowedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.SystemException"})
	public void WSTXLPS301BFVT() throws Exception {
		recoveryTest("3012","server2");
		if (server != null && server.isStarted()) {
			server.stopServer("WTRN0049W"); //ensure server has stopped
		}
	}
	
  @Mode(TestMode.LITE)
	@Test
	public void WSTXLPS301CFVT() throws Exception {
		recoveryTest("3013","both");
	}
	
	@Test
	public void WSTXLPS302AFVT() throws Exception {
		recoveryTest("3021","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.SystemException"})
	public void WSTXLPS302BFVT() throws Exception {
		recoveryTest("3022","server2");
		if (server != null && server.isStarted()) {
			server.stopServer("WTRN0049W"); //ensure server has stopped
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
        String logKeyword = "Jordan said in test: ";
		System.out.println(logKeyword + "========== recoveryTest " + id
				+ " start ==========");
		
        try {
            // We expect this to fail since it is gonna crash the server
        	System.out.println(logKeyword + "callSetupServlet "
					+ id + " start");
        	result = callSetupServlet(id);
        	System.out.println(logKeyword + "callSetupServlet "
					+ id + " end");
        } catch (Throwable e) {
        	System.out.println("Server(s) should be killed. Next step is restarting server(s)");
        }
        Log.info(this.getClass(), method, "setupRec" + id + " returned: " + result);
        
        //restart server in three modes
        if(startServer.equals("server1")){
        		restartServer(server);
                server.waitForStringInTrace("Performed recovery for "+server.getServerName());
        } else if(startServer.equals("server2")){
        		restartServer(server2);
                server2.waitForStringInTrace("Performed recovery for "+server2.getServerName());
        } else if(startServer.equals("both")){
        		restartServer(server);
            	restartServer(server2);
                server.waitForStringInTrace("Performed recovery for "+server.getServerName());
                server2.waitForStringInTrace("Performed recovery for "+server2.getServerName());
        }
        System.out.println(logKeyword + "restarted server: " + startServer);

        Log.info(this.getClass(), method, "calling checkRec" + id);
        try
        {
        	System.out.println(logKeyword + "callCheckServlet "
					+ id + " start");
        	result = callCheckServlet(id);
        	System.out.println(logKeyword + "callCheckServlet "
					+ id + " end");
        } catch (Exception e)
        {
            Log.error(this.getClass(), method, e);
            throw e;
        }
        Log.info(this.getClass(), method, "checkRec" + id + " returned: " + result);
        System.out.println(logKeyword + "checkRec" + id + " returned: " + result);
    }
	
	
	private String callSetupServlet(String testNumber) throws IOException{
			int expectedConnectionCode = HttpURLConnection.HTTP_OK;
			String servletName = "MultiRecoverySetupServlet";
			int test_number = Integer.parseInt(testNumber);
			if (test_number == 1)
				expectedConnectionCode = HttpURLConnection.HTTP_NOT_FOUND;
			String urlStr = BASE_URL + "/recoveryClient/" + servletName
						+ "?number=" + testNumber + "&baseurl=" + BASE_URL
						+ "&baseurl2=" + BASE_URL2;

			System.out.println("callSetupServlet URL: " + urlStr);
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
			System.out.println("Recover test " + testNumber + " Result : " + result);
			assertTrue("Cannot get expected reply from server",
					!result.contains("failed"));
		return "";
	}

	private String callCheckServlet(String testNumber) throws IOException {
		int expectedConnectionCode = HttpURLConnection.HTTP_OK;
		String servletName = "MultiRecoveryCheckServlet";
		//check XAResource status on server1
		String urlStr1 = BASE_URL + "/recoveryServer/" + servletName
				+ "?number=" + testNumber+"01";
		//check XAResource status on server2
		String urlStr2 = BASE_URL2 + "/recoveryServer/" + servletName
				+ "?number=" + testNumber+"02";
		System.out.println("callCheckServlet URL1: " + urlStr1);
		System.out.println("callCheckServlet URL2: " + urlStr2);
		String result1 = "";
		HttpURLConnection con1 = HttpUtils.getHttpConnection(new URL(urlStr1), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try{
	        BufferedReader br1 = HttpUtils.getConnectionStream(con1);
	        result1 = br1.readLine();
		}finally{
			con1.disconnect();
		}
		String result2 = "";
		HttpURLConnection con2 = HttpUtils.getHttpConnection(new URL(urlStr2), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try{
	        BufferedReader br2 = HttpUtils.getConnectionStream(con2);
	        result2 = br2.readLine();
		}finally{
			con2.disconnect();
		}
        assertNotNull(result1);
        assertNotNull(result2);
		System.out.println("Recover test " + testNumber + 
				"\n Result1 : " + result1 + 
				"\n Result2 : " + result2);
		if (testNumber.startsWith("13") || testNumber.startsWith("15") || testNumber.startsWith("16")) {
			assertTrue("All XAResources should rollback but do not get "
					+ "allRollback in the result.", result2.contains("allRollback"));
		}else if (testNumber.startsWith("14")) {
			assertTrue("All XAResources should commit but do not get "
					+ "allCommit in the result.", result2.contains("allCommitted"));
		}else if (testNumber.equals("3011") || testNumber.equals("3021")) {
			assertTrue("Can not get the One Phase XAResource in STARTED state", result1.contains("The One Phase XAResource is in STARTED state."));
//			assertTrue("Can not get the XAResource in ROLLEDBACK state", result2.contains("Get expected one XAResource") &&
//					result2.contains("The XAResource is in ROLLEDBACK state."));
			assertTrue("Can not get the XAResource in ROLLEDBACK state", result2.contains("allRollback"));
		}else if (testNumber.equals("3012") || testNumber.equals("3022")) {
			assertTrue("Can not get the One Phase XAResource in ROLLEDBACK state on server1", 
					result1.contains("The One Phase XAResource is in ROLLEDBACK state"));
			assertTrue("Can not get the XAResource in ROLLEDBACK state on server2", result2.contains("allRollback"));
		}else if (testNumber.equals("3013") || testNumber.equals("3023")) {
			assertTrue("Can not get the One Phase XAResource in STARTED state", result1.contains("The One Phase XAResource is in STARTED state."));
			assertTrue("Can not get the XAResource in ROLLEDBACK state on server2", result2.contains("allRollback"));
		}else if (testNumber.equals("3031")) {
			assertTrue("Can not get the One Phase XAResource in ROLLEDBACK state on server1", 
					result1.contains("The One Phase XAResource is in ROLLEDBACK state"));
//			assertTrue("Can not get the XAResource in ROLLEDBACK state", result2.contains("Get expected one XAResource") &&
//					result2.contains("The XAResource is in ROLLEDBACK state."));
			assertTrue("Can not get the XAResource in ROLLEDBACK state on server2", result2.contains("allRollback"));
		}
		else {
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
	
	private boolean restartServer(LibertyServer server) throws Exception{
		final String method = "restartServer";
		server.waitForStringInLog("Dump State:");
		System.out.println("Restart Server " + server.getServerName());
		server.stopServer(false, false);
		ProgramOutput po = server.startServerAndValidate(false, false, true);
        if (po.getReturnCode() != 0) {
        	server.stopServer(true, true);
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());

            // It may be that we attempted to restart the server too soon.
            Log.info(this.getClass(), method, "start server " +
            		server.getServerName() + " failed, sleep then retry");
            Thread.sleep(30000); // sleep for 30 seconds
            po = server.startServerAndValidate(false, true, true);

            // If it fails again then we'll report the failure
            if (po.getReturnCode() != 0)
            {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server '"  + server.getServerName() + "'");
                Log.error(this.getClass(), "recoveryTest", ex);
                throw ex;
            }
        }
		return true;
	}
	
}
