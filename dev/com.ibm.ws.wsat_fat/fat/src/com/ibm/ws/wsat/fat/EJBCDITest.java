/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class EJBCDITest extends DBTestBase {

	public static final String testURL = "/wsatEJBCDIApp/ClientServlet";
	public static final String testURL2 = "/wsatEJBCDIApp/EJBClientServlet";
	public static final String testURL3 = "/wsatEJBCDIApp/EJBClientServlet2";

	// Server Information
	public static String serverRollbackResult = "Throw exception for rollback from server side!";
	public static String ejbServerRollbackException = "Server returned HTTP response code: 500 for URL";

	@BeforeClass
	public static void beforeTests() throws Exception {

		// Test URL
		appName = "wsatEJBCDIApp";

		client = LibertyServerFactory
				.getLibertyServer("WSATEJB_Client");
		server1 = LibertyServerFactory
				.getLibertyServer("WSATEJB_Server1");
		server1.setHttpDefaultPort(server1Port);
		
		DBTestBase.initWSATTest(client);
		DBTestBase.initWSATTest(server1);

    ShrinkHelper.defaultDropinApp(client, "wsatEJBCDIApp", "com.ibm.ws.wsat.ejbcdi.*");
    ShrinkHelper.defaultDropinApp(server1, "wsatEJBCDIApp", "com.ibm.ws.wsat.ejbcdi.*");
		
		CLient_URL = "http://" + client.getHostname() + ":"
				+ client.getHttpDefaultPort();
		Server1_URL = "http://" + server1.getHostname() + ":"
				+ server1.getHttpDefaultPort();

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
		DBTestBase.cleanupWSATTest(server1);
	}
	
	/**
	 * Test name: testAAA_BBB_CCC: AAA is client side implementation, BBB is server side implementation, CCC is transaction type
	 * Such as testServlet_EJB_ER
	 */

	
	/**
	 * Servlet + EJBWS Tests
	 */

	// EJB with Required
	/**
	 * Client:Servlet, Server1: EJB+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_EJBWS_ER_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "er=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}

	/**
	 * Client:Servlet, Server1: EJB+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client rollback
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_EJBWS_ER_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "er=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
				+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "0");
	}
	
	/**
	 * Client:Servlet, Server1: EJB+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client catch the exception but no action in the end
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	@ExpectedFFDC(value = { "javax.transaction.TransactionRolledbackException", "javax.xml.ws.WebServiceException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
	public void testServlet_EJBWS_ER_Server1Rollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "er=" + rollback + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	/**
	 * Client:Servlet, Server1: EJB+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.TransactionRolledbackException", "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.xml.ws.WebServiceException", "javax.transaction.RollbackException" })
	public void testServlet_EJBWS_ER_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "er=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	// EJB with Requires_New
	/**
	 * Client:Servlet, Server1: EJB+WS with Requires_New attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_EJBWS_EW_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "ew=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * 	REQUIRES_NEW:
		C: start tran
		C: db update 1
		C: call REQUIRESNEW ws
		W: db update 2
		C: rollback tran
		
		Expected Result: check that db update 1 was rolled back but db update 2 was committed -  shows that the WS runs under a different transaction than the client.
		Actual Result: Correct
	 */
	@Test
	public void testServlet_EJBWS_EW_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "ew=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0", "1");
	}

	@Test
	/**
	 * Client:Servlet, Server1: EJB+WS with Requires_New attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 0. New server tx rollsback cos of exc. wsat tran commits.
	 * Actual Result: Client is 1, Server1 is 0
	 * 
	 */
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException"})
	public void testServlet_EJBWS_EW_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "ew=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "1", "0");
	}
	
	// EJB with Mandatory
	/**
	 * Client:Servlet, Server1: EJB+WS with Mandatory attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_EJBWS_EM_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "em=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * 	MANDATORY:
		C: start tran
		C: call MANDATORY ws (expect successful)
		W: doesn't need to do anything
		C: rollback tran
		
		Expected Result: Client DB count is 0, Server1 DB count is 0
		Actual Result: Correct
		
		C: call same MANDATORY ws (expect exception) -- Jordan: Need to modify test code to invoke twice times
	 */
	@Test
	public void testServlet_EJBWS_EM_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "em=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	/**
	 * Client:Servlet, Server1: EJB+WS with Mandatory attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.xml.ws.WebServiceException", "javax.transaction.TransactionRolledbackException", "javax.transaction.RollbackException" })
	public void testServlet_EJBWS_EM_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "em=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	// EJB with Never
	/**
	 * Client:Servlet, Server1: EJB+WS with Never attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0 and throw TX_NEVER Expection
	 * Actual Result: Correct
	 * 
	 */
	@Test
	@ExpectedFFDC(value = {"java.rmi.RemoteException", "com.ibm.websphere.csi.CSIException"})
	public void testServlet_EJBWS_EN_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "en=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "java.rmi.RemoteException: TX_NEVER method called within a global tx; nested exception is: ", "0");
	}
	
	// EJB with Supports
	/**
	 * Client:Servlet, Server1: EJB+WS with Supports attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_EJBWS_ES_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "es=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * SUPPORTS:
		C: start tran
		C: db update 1
		C: call SUPPORTS ws
		W: db update 2
		C: rollback tran
		
		Expected Result: Check Client DB count is 0 and Server1 DB count is 0
		Actual Result: Correct
		
		C: call SUPPORTS ws -- Jordan: cannot do it right now
		W: check ws is not running in a tran

		check that db update 1 and db update 2 were both rolled back
	 */
	@Test
	public void testServlet_EJBWS_ES_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "es=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	/**
	 * Client:Servlet, Server1: EJB+WS with Supports attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException", "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.transaction.TransactionRolledbackException", "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testServlet_EJBWS_ES_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "es=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	// EJB with Not_Supported
	/**
	 * Client:Servlet, Server1: EJB+WS with Not_Supported attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1 cos of autocommit
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_EJBWS_EO_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "eo=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1", "1");
	}
	
	/**
	 * NOT_SUPPORTED:
		C: start tran
		C: call NOTSUPPORTED ws
		W: check ws is not running in a tran
		C: rollback tran
		
		Expected Result: Check Client DB count is 0 and Server1 DB count is 1
		Actual Result: Correct
	 */
	@Test
	public void testServlet_EJBWS_EO_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "eo=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0", "1");
	}
	
	@Test
	/**
	 * Client:Servlet, Server1: EJB+WS with Not_Supported attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1 due to autocommit
	 * Actual Result: Client is 1, Server1 is 1
	 * 
	 */
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException","com.ibm.ws.LocalTransaction.RolledbackException"})
	public void testServlet_EJBWS_EO_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "eo=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "1", "1");
	}
	
	/**
	 * Servlet + CDIWS Tests
	 */
	
	// CDI with Required
	/**
	 * Client:Servlet, Server1:CDI+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CR_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cr=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}

	/**
	 * Client:Servlet, Server1:CDI+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client rollback
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CR_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cr=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
				+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "0");
	}
	
	/**
	 * Client:Servlet, Server1:CDI+WS with Required attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CR_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cr=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, serverRollbackResult, "1", "1");
	}
	
	// CDI with Requires_New
	/**
	 * Client:Servlet, Server1:CDI+WS with Requires_New attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CW_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cw=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}

	@Test
	/**
	 * Client:Servlet, Server1:CDI+WS with Requires_New attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 0 since ws exception sets rollbackonly
	 * Actual Result: Client is 0, Server1 is 0
	 * 
	 * Update 2016-02-13
	 * Jordan: get new result, expected ffdc: javax.xml.ws.WebServiceException
	 * 
	 */
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException"})
	public void testServlet_CDIWS_CW_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cw=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, serverRollbackResult, "1", "0");
	}

	@Test
	/**
	 * 	REQUIRES_NEW:
		C: start tran
		C: db update 1
		C: call REQUIRESNEW CDI ws
		W: db update 2
		C: rollback tran
		
		Expected Result: check that db update 1 was rolled back but db update 2 was committed -  shows that the WS runs under a different transaction than the client.
		Actual Result: Correct
	 */
	public void testServlet_CDIWS_CW_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cw=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0", "1");
	}
	
	// CDI with Mandatory
	/**
	 * Client:Servlet, Server1:CDI+WS with Mandatory attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CM_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cm=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * 	MANDATORY:
		C: start tran
		C: call MANDATORY ws (expect successful)
		W: doesn't need to do anything
		C: rollback tran
		
		Expected Result: Client DB count is 0, Server1 DB count is 0
		Actual Result: Correct
		
		C: call same MANDATORY ws (expect exception) -- Jordan: Need to modify test code to invoke twice times
	 */
	@Test
	public void testServlet_CDIWS_CM_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cm=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	/**
	 * Client:Servlet, Server1:CDI+WS with Mandatory attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 *
	 * WebServiceException is a RuntimeException so global tran will be set RBO
	 */
	@Test
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException", "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void testServlet_CDIWS_CM_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cm=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, serverRollbackResult, "0");
	}
	
	// CDI with Never
	/**
	 * Client:Servlet, Server1:CDI+WS with Never attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0 with Exception
	 * Actual Result: But Client is 1, Server1 is 1
	 * 
	 * 
	 * Update 2016-02-13
	 * Jordan: get new result: TxType.NEVER method called within a global tx
	 */
	@Test
	public void testServlet_CDIWS_CN_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cn=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "TxType.NEVER method called within a global tx", "0");
	}
	
	// CDI with Supports
	/**
	 * Client:Servlet, Server1:CDI+WS with Supports attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CS_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cs=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * SUPPORTS:
		C: start tran
		C: db update 1
		C: call SUPPORTS ws
		W: db update 2
		C: rollback tran
		
		Expected Result: Client DB count is 0, Server1 DB count is 0
		Actual Result: Correct
		
		C: call SUPPORTS ws -- Jordan: cannot do it right now
		W: check ws is not running in a tran

		check that db update 1 and db update 2 were both rolled back
	 */
	@Test
	public void testServlet_CDIWS_CS_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cs=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0");
	}
	
	/**
	 * Client:Servlet, Server1:CDI+WS with Supports attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0
	 * Actual Result: Correct
	 * 
	 * Update 2016-02-13
	 * Jordan: get new result, expected ffdc: javax.xml.ws.WebServiceException
	 */
	@Test
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException", "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void testServlet_CDIWS_CS_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "cs=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, serverRollbackResult, "0");
	}
	
	// CDI with Not_Supported
	/**
	 * Client:Servlet, Server1:CDI+WS with Not_Supported attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testServlet_CDIWS_CO_AllCommit() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "co=" + commit + ":" + basicURL + ":"
				+ server1Port;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * NOT_SUPPORTED:
		C: start tran
		C: call NOTSUPPORTED ws
		W: check ws is not running in a tran
		C: rollback tran
		
		Expected Result: Client DB count is 0, Server1 DB count is 1
		Actual Result: Correct
		
		Update 2016-02-13
		Jordan: get new result, server1 is not rollback
	 */
	@Test
	public void testServlet_CDIWS_CO_ClientRollback() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "co=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "", "0", "1");
	}
	
	@Test
	/**
	 * Client:Servlet, Server1:CDI+WS with Not_Supported attribute
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client commit in catch
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0 because of the fault
	 * Actual Result: Client is 0, Server1 is 0
	 * 
	 * Update 2016-02-13
	 * Jordan: get new result, expected ffdc: javax.xml.ws.WebServiceException
	 */
	@ExpectedFFDC(value = { "javax.xml.ws.WebServiceException"})
	public void testServlet_CDIWS_CO_ClientCommitInCatch() {
		String wsatURL = CLient_URL + testURL + "?" + server1Name
				+ "co=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&" + clientName + "=" + "commitincatch";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, serverRollbackResult, "1");
	}
	
	/**
	 * EJB + Normal WS Tests
	 */
	/**
	 * Client:EJB, Server1:Normal WS
	 * 1, Client in EJB DOESN'T start a transaction, uses EJB transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testEJB_WS_AllCommit() {
		String wsatURL = CLient_URL + testURL2 + "?" + server1Name
				+ "wn=" + commit + ":" + basicURL + ":"
				+ server1Port + "&withouttrans=true";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}

	/**
	 * Client:EJB, Server1:Normal WS
	 * 1, Client in EJB DOESN'T start a transaction, uses EJB transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception
	 * 4, Client catch exception but do nothing in the end
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testEJB_WS_Server1Rollback() {
		String wsatURL = CLient_URL + testURL2 + "?" + server1Name
				+ "wn=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&withouttrans=true";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, serverRollbackResult, "1");
	}
	
	/**
	 * EJB + EJBWS Tests
	 */
	/**
	 * Client:EJB, Server1:EJB+WS
	 * 1, Client in Servlet DOESN't start a transaction, uses EJB transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testEJB_EJBWS_ER_AllCommit() {
		String wsatURL = CLient_URL + testURL2 + "?" + server1Name
				+ "er=" + commit + ":" + basicURL + ":"
				+ server1Port + "&withouttrans=true";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}

	@Test
	/**
	 * Client:EJB, Server1:EJB+WS
	 * 1, Client in Servlet DOESN't start a transaction, uses EJB transaction
	 * 2, Call a service on Server1
	 * 3, Server1 throw WebService Exception for rollback
	 * 4, Client catch exception but do nothing in the end
	 * 
	 * Expected Result: Check Client DB count is 0 and Server0 DB count is 0
	 * Actual Result: Correct
	 * 
	 */
	@ExpectedFFDC(value = { "javax.transaction.RollbackException", "javax.transaction.TransactionRolledbackException", "javax.xml.ws.WebServiceException", "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.transaction.xa.XAException", "javax.ejb.EJBTransactionRolledbackException" })
	public void testEJB_EJBWS_ER_Server1Rollback() {
		String wsatURL = CLient_URL + testURL2 + "?" + server1Name
				+ "er=" + rollback + ":" + basicURL + ":"
				+ server1Port + "&withouttrans=true";
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_INTERNAL_ERROR, "", "0");
	}
	
	@Test
	/**
	 * Client:EJB, Server1:EJB+WS
	 * 1, Client in Servlet starts a transaction
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0, report exception
	 * Actual Result: Correct
	 * 
	 */
	public void testEJB_EJB_ER_AllCommit_NestedUserTransaction() {
		String wsatURL = CLient_URL + testURL2 + "?" + server1Name
				+ "er=" + commit + ":" + basicURL + ":"
				+ server1Port ;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, "The UserTransaction interface is not available to enterprise beans with container-managed transaction demarcation.The EJBClient bean in the wsatEJBCDIApp.war of the wsatEJBCDIApp application uses container-managed transactions.", "0");
	}
	
	/**
	 * Client:EJB, Server1:EJB+WS
	 * 1, Client in Servlet starts a transaction, with BEAN attribute
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client commit
	 * 
	 * Expected Result: Check Client DB count is 1 and Server1 DB count is 1, report exception
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testEJB_EJB_ER_AllCommit_UserTransaction() {
		String wsatURL = CLient_URL + testURL3 + "?" + server1Name
				+ "er=" + commit + ":" + basicURL + ":"
				+ server1Port ;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "1");
	}
	
	/**
	 * Client:EJB, Server1:EJB+WS
	 * 1, Client in Servlet starts a transaction, with BEAN attribute
	 * 2, Call a service on Server1
	 * 3, Server1 commit
	 * 4, Client rollback
	 * 
	 * Expected Result: Check Client DB count is 0 and Server1 DB count is 0, report exception
	 * Actual Result: Correct
	 * 
	 */
	@Test
	public void testEJB_EJB_ER_ClientRollback_UserTransaction() {
		String wsatURL = CLient_URL + testURL3 + "?" + server1Name
				+ "er=" + commit + ":" + basicURL + ":"
				+ server1Port + "&" +clientName + "="
						+ rollback;
		twoServersTest(appName, wsatURL, HttpURLConnection.HTTP_OK, goodResult, "0");
	}
	
	public void twoServersTest(String appName, String testURL, int httpCode, String expectResult,
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

			String result = "";
			if (httpCode == HttpURLConnection.HTTP_OK) {
				result = executeWSAT(testURL);
			} else if (httpCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
				result = executeWSATWithException(testURL, HttpURLConnection.HTTP_INTERNAL_ERROR);
			}
			if(expectResult!=null && expectResult!=""){
				if(httpCode == HttpURLConnection.HTTP_OK){
				assertTrue("Check result, expect is " + expectResult
						+ ", result is " + result, expectResult.equals(result));
				} else if(httpCode == HttpURLConnection.HTTP_INTERNAL_ERROR){
					assertTrue("Check if expect result includes, expect is " + expectResult
							+ ", result is " + result, result.contains(expectResult));
				}
			}

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

	public String executeWSATWithException(String url, int httpCode) {
		String result = "";
		HttpURLConnection con;
		try {
			con = getHttpConnection(new URL(url),
					HttpURLConnection.HTTP_INTERNAL_ERROR, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			result = e.getMessage();
			e.printStackTrace();
		}

		System.out.println("Execute WS-AT test from " + url);
		System.out.println("Result: " + result);
		return result;
	}
}
