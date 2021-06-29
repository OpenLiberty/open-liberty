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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

public class MultiRecoveryTest {
	protected static String BASE_URL;
	protected static String BASE_URL2;

	private final static int REQUEST_TIMEOUT = 10;
	private final static int START_TIMEOUT = 300000; // in ms
	
	private final static String recoveryClient = "recoveryClient";
	private final static String recoveryServer = "recoveryServer";

	public static void beforeTests(LibertyServer server, LibertyServer server2) throws Exception {
		BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
		server2.setHttpDefaultPort(9992);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		ShrinkHelper.defaultDropinApp(server, recoveryClient, "com.ibm.ws.wsat.fat.client.recovery.*");
		ShrinkHelper.defaultDropinApp(server, recoveryServer, "com.ibm.ws.wsat.fat.server.*");
		ShrinkHelper.defaultDropinApp(server2, recoveryClient, "com.ibm.ws.wsat.fat.client.recovery.*");
		ShrinkHelper.defaultDropinApp(server2, recoveryServer, "com.ibm.ws.wsat.fat.server.*");

		server.setServerStartTimeout(START_TIMEOUT);
		server2.setServerStartTimeout(START_TIMEOUT);
	}

	protected void startServers(LibertyServer ... servers) {
		final String method = "startServers";
		
		for (LibertyServer server : servers) {
			assertNotNull("Attempted to start a null server", server);
			try {
				final ProgramOutput po = server.startServerAndValidate(false,false,true);
				if (po.getReturnCode() != 0) {
					Log.info(getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
					Log.info(getClass(), method, "Stdout: " + po.getStdout());
					Log.info(getClass(), method, "Stderr: " + po.getStderr());
					Exception ex = new Exception("Server start failed for "  + server.getServerName());
					throw ex;
				}
			} catch (Exception e) {
				Log.error(getClass(), method, e);
				fail(e.getMessage());
			}
		}
	}
	
	protected void stopServers(LibertyServer ... servers) {
		final String method = "stopServers";

		for (LibertyServer server : servers) {
			if (server == null) {
				Log.info(getClass(), method, "Attempted to stop a null server");
				continue;
			}

			try {
				final ProgramOutput po = server.stopServer(true, true, "WTRN0046E", "WTRN0048W", "WTRN0049W", "WTRN0094W");
				if (po == null) {
					Log.info(getClass(), method, "Attempt to stop " + server.getServerName() + " returned null");
					continue;
				}
				if (po.getReturnCode() != 0) {
					Log.info(getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
					Log.info(getClass(), method, "Stdout: " + po.getStdout());
					Log.info(getClass(), method, "Stderr: " + po.getStderr());
					Exception ex = new Exception("Server stop failed for "  + server.getServerName());
					throw ex;
				}
			} catch (Exception e) {
				Log.error(getClass(), method, e);
			}
		}
	}

	protected void recoveryTest(LibertyServer server, LibertyServer server2, String id, String startServer) throws Exception {
        final String method = "recoveryTest";
        String result = null;

        try {
            // We expect this to fail since it is gonna crash the server
        	result = callSetupServlet(id);
        } catch (Throwable e) {
            Log.info(this.getClass(), method, "callSetupServlet(" + id + ") crashed as expected");
            Log.error(this.getClass(), method, e); 
        }
        Log.info(this.getClass(), method, "callSetupServlet(" + id + ") returned: " + result);

        final String str = "Performed recovery for ";
        final String failMsg = " did not perform recovery";
        //restart server in three modes
        if(startServer.equals("server1")){
        		startServers(server);
                assertNotNull(server.getServerName()+failMsg, server.waitForStringInTrace(str+server.getServerName()));
        } else if(startServer.equals("server2")){
        		startServers(server2);
                assertNotNull(server2.getServerName()+failMsg, server2.waitForStringInTrace(str+server2.getServerName()));
        } else if(startServer.equals("both")){
        		startServers(server, server2);
                assertNotNull(server.getServerName()+failMsg, server.waitForStringInTrace(str+server.getServerName()));
                assertNotNull(server2.getServerName()+failMsg, server2.waitForStringInTrace(str+server2.getServerName()));
        }

		try {
        	result = callCheckServlet(id);
        } catch (Exception e) {
            Log.error(getClass(), method, e);
            throw e;
        }
        Log.info(getClass(), method, "callCheckServlet(" + id + ") returned: " + result);
    }

	protected String callSetupServlet(String testNumber) throws IOException{
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
		try{
			BufferedReader br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
		}finally{
			con.disconnect();
		}
		assertNotNull(result);
		Log.info(getClass(), method, "Recovery test " + testNumber + " Result : " + result);
		assertTrue("Cannot get expected reply from server",
				!result.contains("failed"));
		return "";
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

		Log.info(getClass(), method, "URL1: " + urlStr1 + "\nURL2: " + urlStr2);
		String result1 = "";
		HttpURLConnection con1 = HttpUtils.getHttpConnection(new URL(urlStr1), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try {
	        BufferedReader br1 = HttpUtils.getConnectionStream(con1);
	        result1 = br1.readLine();
		} finally {
			con1.disconnect();
		}
        assertNotNull(result1);

        String result2 = "";
		HttpURLConnection con2 = HttpUtils.getHttpConnection(new URL(urlStr2), 
				expectedConnectionCode, REQUEST_TIMEOUT);
		try {
	        BufferedReader br2 = HttpUtils.getConnectionStream(con2);
	        result2 = br2.readLine();
		} finally {
			con2.disconnect();
		}
        assertNotNull(result2);
        
        Log.info(getClass(), method, "Recovery test " + testNumber + 
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
}
