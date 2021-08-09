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
package com.ibm.ws.wsat.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class LPSTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {
		server = LibertyServerFactory
				.getLibertyServer("MigrationServer1");
		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();
		server2 = LibertyServerFactory
				.getLibertyServer("MigrationServer2");
		server2.setHttpDefaultPort(9992);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		if (server != null && server.isStarted()){
			server.stopServer();
		}
		
		if (server2 != null && server2.isStarted()){
			server2.stopServer();
		}

 		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);
		
    ShrinkHelper.defaultDropinApp(server, "LPSClient", "com.ibm.ws.wsat.lpsclient.*");
    ShrinkHelper.defaultDropinApp(server, "LPSServer", "com.ibm.ws.wsat.lpsserver.*");
    ShrinkHelper.defaultDropinApp(server2, "LPSServer", "com.ibm.ws.wsat.lpsserver.*");

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
	
	// LPS Enabled Test
	@Test
	public void testWSTXLPS101FVT() {
		callServlet("WSTXLPS101FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void testWSTXLPS102FVT() {
		callServlet("WSTXLPS102FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "java.lang.IllegalStateException" })
	public void testWSTXLPS103FVT() {
		callServlet("WSTXLPS103FVT");
	}

	@Test
	public void testWSTXLPS104FVT() {
		callServlet("WSTXLPS104FVT");
	}

	@Test
	public void testWSTXLPS105FVT() {
		callServlet("WSTXLPS105FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	public void testWSTXLPS106FVT() {
		callServlet("WSTXLPS106FVT");
	}

	@Test
	public void testWSTXLPS107FVT() {
		callServlet("WSTXLPS107FVT");
	}

	@Test
	public void testWSTXLPS108FVT() {
		callServlet("WSTXLPS108FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	public void testWSTXLPS109FVT() {
		callServlet("WSTXLPS109FVT");
	}

	@Test
	public void testWSTXLPS110FVT() {
		callServlet("WSTXLPS110FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS111FVT() {
		callServlet("WSTXLPS111FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS112FVT() {
		callServlet("WSTXLPS112FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS113FVT() {
		callServlet("WSTXLPS113FVT");
	}

	@Test
	@ExpectedFFDC(value = { "java.lang.IllegalStateException" })
	public void testWSTXLPS114FVT() {
		callServlet("WSTXLPS114FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void testWSTXLPS201FVT() {
		callServlet("WSTXLPS201FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS202FVT() {
		callServlet("WSTXLPS202FVT");
		assertNotNull("Expected to see Error WTRN0064E", server2.waitForStringInLog("WTRN0064E"));
		List<String> errors = new ArrayList<String>();
		errors.add("WTRN0064E");
		server2.addIgnoredErrors(errors);
	}
	
	@Test
	public void testWSTXLPS203FVT() {
		callServlet("WSTXLPS203FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS204FVT() {
		callServlet("WSTXLPS204FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSTXLPS205FVT() {
		callServlet("WSTXLPS205FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException"})
	public void testWSTXLPS206FVT() {
		callServlet("WSTXLPS206FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	public void testWSTXLPS207FVT() {
		callServlet("WSTXLPS207FVT");
	}

	private void callServlet(String testMethod){
		try {
			int testNumber = Integer.parseInt(testMethod.substring(7, 10));
			String providerURL = BASE_URL;
			if (testNumber>200)
				providerURL = BASE_URL2;
			String urlStr = BASE_URL + "/LPSClient/LPSClientServlet"
					+ "?method=" + testNumber + "&baseurl=" + providerURL;
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
