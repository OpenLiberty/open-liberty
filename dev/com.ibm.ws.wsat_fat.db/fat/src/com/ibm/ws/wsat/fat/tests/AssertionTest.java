/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class AssertionTest extends WSATTest {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("WSATBasic");
	private static String BASE_URL = "http://" + server.getHostname() + ":"
			+ server.getHttpDefaultPort();
	private static final String contextRoot = "/assertion";

	@BeforeClass
	public static void beforeTests() throws Exception {
		DBTestBase.initWSATTest(server);

		ShrinkHelper.defaultDropinApp(server, "assertion", "com.ibm.ws.wsat.assertion.*");

		FATUtils.startServers(server);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server);

		DBTestBase.cleanupWSATTest(server);
	}

	@Test
	public void testNoPolicyAssertion() {
		String method = "testNoPolicyAssertion";
		// We have change the behavior, if enable wsat feature and exist trans, then we will use global transaction
		// DELETE ME: Should work as normal web service and no exception is expected
		try {
			String urlStr = BASE_URL + contextRoot + "/AssertionClientServlet"
					+ "?baseurl=" + BASE_URL;
			Log.info(getClass(), method, urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testNoPolicyAssertion");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Reply from server: Hello World!"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testNoPolicyAssertionNoTransaction() {
		String method = "testNoPolicyAssertionNoTransaction";
		// Should work as normal web service and no exception is expected
		try {
			String urlStr = BASE_URL + contextRoot + "/AssertionClientServlet"
					+ "?baseurl=" + BASE_URL;
			Log.info(getClass(), method, urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testNoPolicyAssertionNoTransaction");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Reply from server: Hello World!"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testAssertionOptional() {
		String method = "testAssertionOptional";
		try {
			String urlStr = BASE_URL + contextRoot + "/AssertionClientServlet"
					+ "?baseurl=" + BASE_URL;
			Log.info(getClass(), method, urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testAssertionOptional");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Reply from server: Hello World!"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testAssertionOptionalNoTransaction() {
		String method = "testAssertionOptionalNoTransaction";
		try {
			String urlStr = BASE_URL + contextRoot + "/AssertionClientServlet"
					+ "?baseurl=" + BASE_URL;
			Log.info(getClass(), method, urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testAssertionOptionalNoTransaction");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Reply from server: Hello World!"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	// CXF 2.6.2 does some work during Servlet.init that causes FFDC to come out,
	// but with CXF 3.x, that logic does not happen any longer, so the FFDCs do not come out.
	// See AbstractHTTPDestination.initConfig to see the difference in behavior.
	@Test
	@ExpectedFFDC(value = { "javax.servlet.ServletException", "java.lang.RuntimeException" },
	              repeatAction = {EmptyAction.ID, EE8FeatureReplacementAction.ID})
	public void testAssertionIgnorable() {
		String method = "testAssertionIgnorable";
		// Expect an exception because Atomic Transaction policy assertion
		// MUST NOT include a wsp:Ignorable attribute with a value of 'true'.
		try {
			String urlStr = BASE_URL + contextRoot + "/AssertionClientServlet"
					+ "?baseurl=" + BASE_URL;
			Log.info(getClass(), method, urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testAssertionIgnorable");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, result);
			server.waitForStringInLog("WS-AT does not accept Ignorable attribute is TRUE");
			List<String> errors = new ArrayList<String>();
			errors.add("SRVE0271E");
			server.addIgnoredErrors(errors);
			/*assertTrue(
					"No exception happens",
					result.contains("The Atomic Transaction policy assertion MUST NOT include"
							+ " a wsp:Ignorable attribute with a value of true."));*/
		} catch (Exception e) {
			System.out.println("Exception happens: " + e.toString());
			e.printStackTrace();
		}
	}
}