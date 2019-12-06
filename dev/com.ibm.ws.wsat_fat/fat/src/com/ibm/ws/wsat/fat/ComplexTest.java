/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@AllowedFFDC(value = { "javax.transaction.SystemException" })
public class ComplexTest extends WSATTest {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("MigrationServer1");
	private static String BASE_URL = "http://" + server.getHostname() + ":"
			+ server.getHttpDefaultPort();
	private static LibertyServer server2 = LibertyServerFactory
			.getLibertyServer("MigrationServer2");
	private static String BASE_URL2 = "http://" + server2.getHostname() + ":9992";
	private static LibertyServer server3 = LibertyServerFactory
			.getLibertyServer("MigrationServer3");
	private static String BASE_URL3 = "http://" + server3.getHostname() + ":9993";
	private final static int REQUEST_TIMEOUT = 60;

	@BeforeClass
	public static void beforeTests() throws Exception {
		DBTestBase.initWSATTest(server);

		if (server != null && server.isStarted()){
			server.stopServer();
		}
		
		if (server2 != null && server2.isStarted()){
			server2.stopServer();
		}
		if (server3 != null && server3.isStarted()){
			server3.stopServer();
		}

        if (server != null && !server.isStarted()){
        	 server.setServerStartTimeout(600000);
             server.startServer(true);
		}
		
		if (server2 != null && !server2.isStarted()){
			 server2.setServerStartTimeout(600000);
		     server2.startServer(true);
		}
		if (server3 != null && !server3.isStarted()){
			 server3.setServerStartTimeout(600000);
		     server3.startServer(true);
		}
	}

	@AfterClass
    public static void tearDown() throws Exception {
        ServerUtils.stopServer(server);
        ServerUtils.stopServer(server2);
        ServerUtils.stopServer(server3);

        DBTestBase.cleanupWSATTest(server);
    }
	
	@Test
	public void testWSATRE064FVT() {
		callServlet("WSATRE064FVT");		
	}
	
	@Test
	public void testWSATRE065FVT() {
		callServlet("WSATRE065FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE066FVT() {
		callServlet("WSATRE066FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE067FVT() {
		callServlet("WSATRE067FVT");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testWSATRE068FVT() {
		callServlet("WSATRE068FVT");
	}
	
	@Test
	public void testWSATRE069FVT() {
		callServlet("WSATRE069FVT");
	}
	
	@Test
	public void testWSATRE070FVT() {
		callServlet("WSATRE070FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE071FVT() {
		callServlet("WSATRE071FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE072FVT() {
		callServlet("WSATRE072FVT");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
	public void testWSATRE073FVT() {
		callServlet("WSATRE073FVT");
	}
	
	private void callServlet(String testMethod){
		try {
			String urlStr = BASE_URL + "/simpleClient/SimpleClientServlet"
					+ "?method=" + Integer.parseInt(testMethod.substring(6, 9))
					+ "&baseurl=" + BASE_URL2 +"&baseurl2=" + BASE_URL3 ;
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
