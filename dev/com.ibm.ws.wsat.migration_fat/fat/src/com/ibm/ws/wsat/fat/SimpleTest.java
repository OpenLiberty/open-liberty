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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
@AllowedFFDC(value = { "javax.transaction.SystemException" })
public class SimpleTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {

		server = LibertyServerFactory.getLibertyServer("MigrationServer1");
		BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();

		server2 = LibertyServerFactory.getLibertyServer("MigrationServer2");
		BASE_URL2 = "http://" + server2.getHostname() + ":9992";

		if (server != null && server.isStarted()){
			server.stopServer();
		}
		
		if (server2 != null && server2.isStarted()){
			server2.stopServer();
		}

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

    ShrinkHelper.defaultDropinApp(server, "simpleClient", "com.ibm.ws.wsat.simpleclient.client.simple");
    ShrinkHelper.defaultDropinApp(server2, "simpleServer", "com.ibm.ws.wsat.simpleserver.server");

    if (server != null && !server.isStarted()){
       server.setServerStartTimeout(600000);
       server.startServer(true);
		}
		
		if (server2 != null && !server2.isStarted()){
			 server2.setServerStartTimeout(600000);
       server2.startServer(true);
		}
	}

	@AfterClass
    public static void tearDown() throws Exception {
		ServerUtils.stopServer(server);
		ServerUtils.stopServer(server2);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
  }
	
	@After
	public void sleep() throws InterruptedException {
		// Sleep a little to ensure stray async messages are all done
		Thread.sleep(5000);
	}

  @Test
	public void testWSATRE001FVT() {
		//Client: Begin Tx, getStatus, Call Web Service, getStatus, Commit Tx
        //Server: getStatus
		callServlet("WSATRE001FVT");		
	}
	
	@Test
	public void testWSATRE002FVT() {
		//Client: Begin Tx, getStatus, Call Web Service, getStatus, Rollback Tx
        //Server: getStatus
		callServlet("WSATRE002FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE003FVT() {
		callServlet("WSATRE003FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE004FVT() {
		callServlet("WSATRE004FVT");
	}
	
	@Test
	public void testWSATRE005FVT() {
		callServlet("WSATRE005FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE006FVT() {
		callServlet("WSATRE006FVT");
	}
	
	@Test
	public void testWSATRE007FVT() {
		callServlet("WSATRE007FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE008FVT() {
		callServlet("WSATRE008FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE009FVT() {
		callServlet("WSATRE009FVT");
	}
	
	@Test
	public void testWSATRE010FVT() {
		callServlet("WSATRE010FVT");
	}
	
	@Test
	public void testWSATRE011FVT() {
		callServlet("WSATRE011FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE012FVT() {
		callServlet("WSATRE012FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE013FVT() {
		callServlet("WSATRE013FVT");
	}
	
	@Test
	public void testWSATRE014FVT() {
		callServlet("WSATRE014FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE015FVT() {
		callServlet("WSATRE015FVT");
	}
	
	@Test
	public void testWSATRE016FVT() {
		callServlet("WSATRE016FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE017FVT() {
		callServlet("WSATRE017FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE018FVT() {
		callServlet("WSATRE018FVT");
	}
	
	@Test
	public void testWSATRE019FVT() {
		callServlet("WSATRE019FVT");
	}
	
	@Test
	public void testWSATRE020FVT() {
		callServlet("WSATRE020FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE021FVT() {
		callServlet("WSATRE021FVT");
	}
	
	@Test
	public void testWSATRE022FVT() {
		callServlet("WSATRE022FVT");
	}
	
	@Test
	public void testWSATRE023FVT() {
		callServlet("WSATRE023FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE024FVT() {
		callServlet("WSATRE024FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE025FVT() {
		callServlet("WSATRE025FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE026FVT() {
		callServlet("WSATRE026FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE027FVT() {
		callServlet("WSATRE027VT");
	}
	
	@Test
	public void testWSATRE028FVT() {
		callServlet("WSATRE028FVT");
	}
	
	@Test
	public void testWSATRE029FVT() {
		callServlet("WSATRE029FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE030FVT() {
		callServlet("WSATRE030FVT");
	}
	
	@Test
	public void testWSATRE031FVT() {
		callServlet("WSATRE031FVT");
	}
	
	@Test
	public void testWSATRE032FVT() {
		callServlet("WSATRE032FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
  @Mode(TestMode.LITE)
	public void testWSATRE033FVT() {
		callServlet("WSATRE033FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE034FVT() {
		callServlet("WSATRE034FVT");
	}
	
	@Test
	public void testWSATRE035FVT() {
		callServlet("WSATRE035FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE036FVT() {
		callServlet("WSATRE036FVT");
	}
	
	@Test
	public void testWSATRE037FVT() {
		callServlet("WSATRE037VT");
	}
	
	@Test
	public void testWSATRE038FVT() {
		callServlet("WSATRE038FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE039FVT() {
		callServlet("WSATRE039FVT");
	}
	
	@Test
	public void testWSATRE040FVT() {
		callServlet("WSATRE040FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE041FVT() {
		callServlet("WSATRE041FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE042FVT() {
		callServlet("WSATRE042FVT");
	}
	
	@Test
	public void testWSATRE043FVT() {
		callServlet("WSATRE043FVT");
	}
	
	@Test
	public void testWSATRE044FVT() {
		callServlet("WSATRE044FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE045FVT() {
		callServlet("WSATRE045FVT");
	}
	
	@Test
	public void testWSATRE046FVT() {
		callServlet("WSATRE046FVT");
	}
	
	@Test
	public void testWSATRE047FVT() {
		callServlet("WSATRE047VT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE048FVT() {
		callServlet("WSATRE048FVT");
	}
	
	@Test
	public void testWSATRE049FVT() {
		callServlet("WSATRE049FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE050FVT() {
		callServlet("WSATRE050FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE051FVT() {
		callServlet("WSATRE051FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE052FVT() {
		callServlet("WSATRE052FVT");
	}
	
	@Test
	public void testWSATRE053FVT() {
		callServlet("WSATRE053FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE054FVT() {
		callServlet("WSATRE054FVT");
	}
	
	@Test
	public void testWSATRE055FVT() {
		callServlet("WSATRE055FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE056FVT() {
		callServlet("WSATRE056FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
  @Mode(TestMode.LITE)
	public void testWSATRE057FVT() {
		callServlet("WSATRE057VT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE058FVT() {
		callServlet("WSATRE058FVT");
	}
	
	@Test
	public void testWSATRE059FVT() {
		callServlet("WSATRE059FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSATRE060FVT() {
		callServlet("WSATRE060FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE061FVT() {
		callServlet("WSATRE061FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE062FVT() {
		callServlet("WSATRE062FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE063FVT() {
		callServlet("WSATRE063FVT");
	}
	
	private void callServlet(String testMethod){
		try {
			String urlStr = BASE_URL + "/simpleClient/SimpleClientServlet"
					+ "?method=" + Integer.parseInt(testMethod.substring(6, 9))
					+ "&baseurl=" + BASE_URL2;
			System.out.println(testMethod + " URL: " + urlStr);
            HttpURLConnection con = getHttpConnection(new URL(urlStr), 
            		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String result = br.readLine();
            assertNotNull(result);
			System.out.println(testMethod + " Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Test passed"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
	
}
