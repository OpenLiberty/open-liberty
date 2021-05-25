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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@AllowedFFDC(value = { "javax.transaction.SystemException", "javax.transaction.xa.XAException" })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MultiRecoveryTest {
	
	
	@Server("WSATRecovery1")
	public static LibertyServer server;
	private static String BASE_URL;
	
	@Server("WSATRecovery2")
	public static LibertyServer server2;
	private static String BASE_URL2;

	private final static int REQUEST_TIMEOUT = 10;
	private final static int START_TIMEOUT = 300000; // in ms
	
	private final static String recoveryClient = "recoveryClient";
	private final static String recoveryServer = "recoveryServer";

	@BeforeClass
	public static void beforeTests() throws Exception {
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

	@Before
	public void beforeTest() throws Exception {
		startServers(server, server2);
	}
	
	private void startServers(LibertyServer ... servers) {
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
	
	private void stopServers(LibertyServer ... servers) {
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

	@After
	public void tearDown() throws Exception {
		stopServers(server, server2);
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR001AFVT() throws Exception {
		recoveryTest("101","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0049W during test
	// Report javax.transaction.SystemException
	public void WSTXMPR001BFVT() throws Exception {
		recoveryTest("102","server2");
	}
	
	@Test
	public void WSTXMPR001CFVT() throws Exception {
		recoveryTest("103","both");
	}
	
	@Test
	public void WSTXMPR002AFVT() throws Exception {
		recoveryTest("201","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0049W and Warning WTRN0046E during test
	// Report javax.transaction.SystemException 
	public void WSTXMPR002BFVT() throws Exception {
		recoveryTest("202","server2");
	}
	
	@Test
	public void WSTXMPR002CFVT() throws Exception {
		recoveryTest("203","both");
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR003AFVT() throws Exception {
		recoveryTest("301","server1");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR003BFVT() throws Exception {
		recoveryTest("302","server2");
	}
	
	@Test
	public void WSTXMPR003CFVT() throws Exception {
		recoveryTest("303","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"/*, "com.ibm.ws.wsat.service.WSATException" */})
	public void WSTXMPR004AFVT() throws Exception {
		recoveryTest("401","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR004BFVT() throws Exception {
		recoveryTest("402","server2");
	}
	
	@Test
	public void WSTXMPR004CFVT() throws Exception {
		recoveryTest("403","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"/*, "com.ibm.ws.wsat.service.WSATException" */})
	public void WSTXMPR005AFVT() throws Exception {
		recoveryTest("501","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0046E and Warning WTRN0049W, WTRN0094W during test
	// Expect XAException and RollbackException
	// Report javax.transaction.SystemException 
	public void WSTXMPR005BFVT() throws Exception {
		recoveryTest("502","server2");
	}
	
	@Test
	//@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR005CFVT() throws Exception {
		recoveryTest("503","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR006AFVT() throws Exception {
		recoveryTest("601","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void WSTXMPR006BFVT() throws Exception {
		recoveryTest("602","server2");
	}
	
	@Test
	public void WSTXMPR006CFVT() throws Exception {
		recoveryTest("603","both");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.SystemException" })
	public void WSTXMPR007AFVT() throws Exception {
		recoveryTest("701","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "java.util.concurrent.RejectedExecutionException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR007BFVT() throws Exception {
		recoveryTest("702","server2");
	}
	
  @Mode(TestMode.LITE)
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Report javax.transaction.SystemException
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR007CFVT() throws Exception {
		recoveryTest("703","both");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.SystemException" })
	public void WSTXMPR008AFVT() throws Exception {
		recoveryTest("801","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR008BFVT() throws Exception {
		recoveryTest("802","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Report javax.transaction.SystemException
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR008CFVT() throws Exception {
		recoveryTest("803","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException", "javax.xml.ws.WebServiceException"})
	public void WSTXMPR009AFVT() throws Exception {
		recoveryTest("901","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = {"javax.transaction.SystemException"})
	//Caused by: javax.transaction.xa.XAException
		//at com.ibm.ws.wsat.tm.impl.ParticipantResource.commit(ParticipantResource.java:114)
		//Perhaps this can be ignored
	public void WSTXMPR009BFVT() throws Exception {
		recoveryTest("902","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	//javax.transaction.xa.XAException 
	//Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	//Need review on whether it is expected
	public void WSTXMPR009CFVT() throws Exception {
		recoveryTest("903","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException", "javax.xml.ws.WebServiceException"})
	public void WSTXMPR010AFVT() throws Exception {
		recoveryTest("1001","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "javax.transaction.SystemException"})
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	// Need Jon Review:
	// Caused by: javax.transaction.xa.XAException
	// at com.ibm.ws.wsat.tm.impl.ParticipantResource.commit(ParticipantResource.java:114)
	// Perhaps this can be ignored
	public void WSTXMPR010BFVT() throws Exception {
		recoveryTest("1002","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Maybe a defect so
	// Add @ExpectedFFDC(value = {"javax.transaction.xa.XAException"})
	// Because javax.transaction.xa.XAException > at com.ibm.tx.jta.embeddable.impl.WSATParticipantWrapper.commit(WSATParticipantWrapper.java:118)
	public void WSTXMPR010CFVT() throws Exception {
		recoveryTest("1003","both");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011AFVT() throws Exception {
		recoveryTest("1101","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011BFVT() throws Exception {
		recoveryTest("1102","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011CFVT() throws Exception {
		recoveryTest("1103","both");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR012AFVT() throws Exception {
		recoveryTest("1201","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = {"javax.transaction.SystemException" })
	public void WSTXMPR012BFVT() throws Exception {
		recoveryTest("1202","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR012CFVT() throws Exception {
		recoveryTest("1203","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR013AFVT() throws Exception {
		recoveryTest("1301","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException","javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	@ExpectedFFDC(value = {"javax.transaction.RollbackException"})
	public void WSTXMPR013BFVT() throws Exception {
		recoveryTest("1302","server2");
	}
	
	@Test
	public void WSTXMPR013CFVT() throws Exception {
		recoveryTest("1303","both");
	}
	
	@Test
	public void WSTXMPR014AFVT() throws Exception {
		recoveryTest("1401","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException"})
	@AllowedFFDC(value = {"javax.transaction.SystemException", "java.util.concurrent.RejectedExecutionException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR014BFVT() throws Exception {
		recoveryTest("1402","server2");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException"})
	public void WSTXMPR014CFVT() throws Exception {
		recoveryTest("1403","both");
	}
	
	@Test
	public void WSTXMPR015AFVT() throws Exception {
		recoveryTest("1501","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException" })
	public void WSTXMPR015BFVT() throws Exception {
		recoveryTest("1502","server2");
	}
	
	@Test
	public void WSTXMPR015CFVT() throws Exception {
		recoveryTest("1503","both");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR016AFVT() throws Exception {
		recoveryTest("1601","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR016BFVT() throws Exception {
		recoveryTest("1602","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR016CFVT() throws Exception {
		recoveryTest("1603","both");
	}
	
	protected void recoveryTest(String id, String startServer) throws Exception {
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

	private String callSetupServlet(String testNumber) throws IOException{
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

	private String callCheckServlet(String testNumber) throws IOException {
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
