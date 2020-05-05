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

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
public class SleepTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;

	@BeforeClass
	public static void beforeTests() throws Exception {

		server = LibertyServerFactory
				.getLibertyServer("WSATSleep");
		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();
		server2 = LibertyServerFactory
				.getLibertyServer("MigrationServer2");
		server2.setHttpDefaultPort(9992);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);

    ShrinkHelper.defaultDropinApp(server, "simpleClient", "com.ibm.ws.wsat.simpleclient.client.simple");
    ShrinkHelper.defaultDropinApp(server2, "simpleServer", "com.ibm.ws.wsat.simpleserver.server");

		if (server != null && server.isStarted()){
			server.stopServer();
		}
		
		if (server2 != null && server2.isStarted()){
			server2.stopServer();
		}

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
	
	@Test
	@Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE094FVT() {
		callServlet("WSATRE094FVT");
	}
	
	@Test
	public void testWSATRE095FVT() {
		callServlet("WSATRE095FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE096FVT() {
		callServlet("WSATRE096FVT");
	}
	
	@Test
	@Mode(TestMode.LITE)
	public void testWSATRE097FVT() {
		callServlet("WSATRE097FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE098FVT() {
		callServlet("WSATRE098FVT");
	}
	
	@Test
	public void testWSATRE099FVT() {
		callServlet("WSATRE099FVT");
	}
	
	@Test
	@Mode(TestMode.LITE)
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void testWSATRE100FVT() {
		callServlet("WSATRE100FVT");
	}
	
	@Test
	public void testWSATRE101FVT() {
		callServlet("WSATRE101FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE102FVT() {
		callServlet("WSATRE102FVT");
	}
	
	@Test
	@Mode(TestMode.LITE)
	public void testWSATRE103FVT() {
		callServlet("WSATRE103FVT");
	}
	
	@Test
	public void testWSATRE104FVT() {
		callServlet("WSATRE104FVT");
	}
	
	@Test
	public void testWSATRE105FVT() {
		callServlet("WSATRE105FVT");
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
			//assertTrue("Cannot get expected Transaction status on server",
			//		result.contains("Status: 0"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}
