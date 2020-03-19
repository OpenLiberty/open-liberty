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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
public class LPSDisabledTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;

	@BeforeClass
	public static void beforeTests() throws Exception {
		
		server = LibertyServerFactory
				.getLibertyServer("LPSDisabled");
		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();

		DBTestBase.initWSATTest(server);

   ShrinkHelper.defaultDropinApp(server, "LPSClient", "com.ibm.ws.wsat.lpsclient.*");
   ShrinkHelper.defaultDropinApp(server, "LPSServer", "com.ibm.ws.wsat.lpsserver.*");

		if (server != null && server.isStarted()){
			server.stopServer();
		}

        if (server != null && !server.isStarted()){
        	 server.setServerStartTimeout(600000);
             server.startServer(true);
		}
	}

	@AfterClass
    public static void tearDown() throws Exception {
		ServerUtils.stopServer(server, "WTRN0062E", "WTRN0063E");

		DBTestBase.cleanupWSATTest(server);
    }
	
	// LPS Disabled Test
	@Test
	public void testWSTXLPS001FVT() {
		callServlet("WSTXLPS001FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void testWSTXLPS002FVT() {
		callServlet("WSTXLPS002FVT");
	}
	
	@Test
  @Mode(TestMode.LITE)
	@ExpectedFFDC(value = { "java.lang.IllegalStateException" })
	public void testWSTXLPS003FVT() {
		callServlet("WSTXLPS003FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.RollbackException" })
	public void testWSTXLPS004FVT() {
		callServlet("WSTXLPS004FVT");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.RollbackException" })
	public void testWSTXLPS005FVT() {
		callServlet("WSTXLPS005FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	public void testWSTXLPS006FVT() {
		callServlet("WSTXLPS006FVT");
	}

	@Test
	public void testWSTXLPS007FVT() {
		callServlet("WSTXLPS007FVT");
	}

	@Test
	public void testWSTXLPS008FVT() {
		callServlet("WSTXLPS008FVT");
	}

	@Test
  @Mode(TestMode.LITE)
	public void testWSTXLPS009FVT() {
		callServlet("WSTXLPS009FVT");
	}

	@Test
	public void testWSTXLPS010FVT() {
		callServlet("WSTXLPS010FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.RollbackException", "java.lang.IllegalStateException" })
	public void testWSTXLPS011FVT() {
		callServlet("WSTXLPS011FVT");
	}
	
	private void callServlet(String testMethod){
		try {
			int testNumber = Integer.parseInt(testMethod.substring(7, 10));
			String providerURL = BASE_URL;
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
