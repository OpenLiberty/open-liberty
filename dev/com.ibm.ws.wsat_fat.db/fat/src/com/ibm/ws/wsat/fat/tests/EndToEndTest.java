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
package com.ibm.ws.wsat.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class EndToEndTest extends WSATTest {
	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("WSATBasic");
	private static String BASE_URL = "http://" + server.getHostname() + ":"
			+ server.getHttpDefaultPort();
	private final static int REQUEST_TIMEOUT = 60;

	@BeforeClass
	public static void beforeTests() throws Exception {
		if (server != null && !server.isStarted()) {
			server.startServer();
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		ServerUtils.stopServer(server);
	}

	@Test
	public void testOneway() {
		try {
			String urlStr = BASE_URL + "/oneway/OnewayClientServlet"
					+ "?type=oneway&baseurl=" + BASE_URL;
			System.out.println("testOneway URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println("testOneway Result : " + result);
			assertTrue(
					"Cannot get expected exception from server",
					result.contains("javax.xml.ws.soap.SOAPFaultException:"
							+ " WS-AT can not work on ONE-WAY webservice method"));
			// List<String> errors = new ArrayList<String>();
			// errors.add("WTRN0127E");
			// server.addIgnoredErrors(errors);
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testTwowayCommit() {
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=twoservercommit&baseurl=" + BASE_URL;
			System.out.println("testTwoway URL: " + urlStr);
			HttpURLConnection con = HttpUtils
					.getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println("testTwoway Result : " + result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Finish Twoway message"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
	
	@Test
	public void testTwowayRollback() {
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=twoserverrollback&baseurl=" + BASE_URL;
			System.out.println("testTwoway URL: " + urlStr);
			HttpURLConnection con = HttpUtils
					.getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println("testTwoway Result : " + result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Finish Twoway message"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testNoOptionalNoTransaction() {
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=noOptionalNoTransaction&baseurl=" + BASE_URL;
			System.out.println("testNoOptionalNoTransaction URL: " + urlStr);
			HttpURLConnection con = HttpUtils
					.getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println("testNoOptionalNoTransaction Result : " + result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Detected WS-AT policy, however there is no"
							+ " active transaction in current thread"));

		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testFeatureDynamic() {
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=twoway&baseurl=" + BASE_URL;
			System.out.println("testFeatureDynamic URL: " + urlStr);
			HttpURLConnection con = HttpUtils
					.getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			System.out.println("testFeatureDynamic First Result : " + result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Finish Twoway message"));
			
			server.stopServer();
			server.startServer();
			server.setServerConfigurationFile("dynamicallyRemoveWSAT/serverWithoutWSAT.xml");
	        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
	        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
	        //assertNotNull("usr:RSHandler1Feature install failed", server.waitForStringInLog("CWWKF0012I: The server installed the following features: \\[usr:RSHandler1Feature\\]"));
	        
	        
	        con = HttpUtils.getHttpConnection(new URL(urlStr),
	        		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
	        br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
			assertNotNull(result);
			System.out.println("testFeatureDynamic Second Result : " + result);
			assertTrue("Cannot get expected reply from server", result.contains("javax.naming.NameNotFoundException"));
	        
			server.stopServer();
			server.startServer();
	        server.setServerConfigurationFile("dynamicallyRemoveWSAT/serverWithWSAT.xml");
	        assertNotNull("Expected to see config update completed", server.waitForStringInLog("CWWKG0017I"));
	        assertNotNull("Expected to see feature update completed", server.waitForStringInLog("CWWKF0008I"));
	        
	        con = HttpUtils.getHttpConnection(new URL(urlStr),
	        		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
	        br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
			assertNotNull(result);
			System.out.println("testFeatureDynamic Third Result : " + result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Finish Twoway message"));
			
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	/*
	 * public String callStringProvider(String endpointUrl) throws IOException,
	 * MalformedURLException { URL url = new URL(endpointUrl); InputStream is =
	 * url.openConnection().getInputStream(); BufferedReader br = new
	 * BufferedReader(new InputStreamReader(is));
	 * 
	 * String line = null; StringBuffer sb = new StringBuffer();
	 * 
	 * while ((line = br.readLine()) != null) { sb.append(line); }
	 * 
	 * return sb.toString(); }
	 */
}
