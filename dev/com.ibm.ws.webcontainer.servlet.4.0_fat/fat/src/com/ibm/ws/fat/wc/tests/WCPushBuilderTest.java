/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
public class WCPushBuilderTest extends LoggingTest {

	private static final Logger LOG = Logger.getLogger(WCPushBuilderTest.class.getName());

	final static String appNameServlet40 = "TestServlet40";

	@ClassRule
	public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

	@BeforeClass
	public static void setUp() throws Exception {

		LOG.info("Setup : add TestServlet40 to the server if not already present.");

		WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestServlet40.ear", true,
				"TestServlet40.war", true, "TestServlet40.jar", true, "testservlet40.war.servlets",
				"testservlet40.war.listeners", "testservlet40.jar.servlets");

		SHARED_SERVER.startIfNotStarted();

		LOG.info("Setup : wait for message to indicate app has started");

		SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* TestServlet40", 10000);

		LOG.info("Setup : complete, ready for Tests");

	}

	@AfterClass
	public static void testCleanup() throws Exception {

		SHARED_SERVER.getLibertyServer().stopServer(null);
	}

	@Test
	@Mode(TestMode.LITE)
	public void testPushBuilderAPI() throws Exception {

		String[] expectedMessages = { "PASS" };
		String[] unExpectedMessages = { "FAIL" };

		this.verifyResponse("/TestServlet40/PushBuilderAPIServlet", expectedMessages, unExpectedMessages);

	}

	@Test
	@Mode(TestMode.LITE)
	public void testPushBuilderHeaders() throws Exception {

		HashMap<String, String> headers = new HashMap<String, String>();

		// Headers which should not be part of a push request
		headers.put("If-Modified-Since", "Tue, 07 Feb 2017 12:50:00 GMT");
		headers.put("Expect", "100-Continue");
		headers.put("Referer", "PushBuilderAPIServlet");

		// Headers which shoul be part of the push request
		headers.put("Content-Type", "charset=pushypushpush");
		headers.put("Date", "Tue, 07 Feb 2017 13050:00 GMT");
		headers.put("From", "pushbuildertest@us.ibm.com");
		headers.put("MaxForwards", "99");

		String out = getResponse("/PushBuilderAPIServlet", headers);

		assertTrue("Fail expected header not found in response : " + "PB Header : Content-Type:charset=pushypushpush",
				out.contains("PB Header : Content-Type:charset=pushypushpush"));
		assertTrue("Fail expected header not found in response : " + "PB Header : Date:Tue, 07 Feb 2017 13050:00 GMT",
				out.contains("PB Header : Date:Tue, 07 Feb 2017 13050:00 GMT"));
		assertTrue("Fail expected header not found in response : " + "PB Header : From:pushbuildertest@us.ibm.com",
				out.contains("PB Header : From:pushbuildertest@us.ibm.com"));
		assertTrue("Fail expected header not found in response : " + "PB Header : MaxForwards:99",
				out.contains("PB Header : MaxForwards:99"));

		assertFalse(
				"Fail unexpected header found in response : "
						+ "PB Header : If-Modified-Since:Tue, 07 Feb 2017 12:50:00 GMT",
				out.contains("PB Header : If-Modified-Since:Tue, 07 Feb 2017 12:50:00 GMT"));
		assertFalse("Fail unexpected header found in response : " + "PB Header : Expect:100-Continue",
				out.contains("PB Header : Expect:100-Continue"));
		assertFalse("Fail unexpected header found in response : " + "PB Header : Referer:PushBuilderAPIServlet",
				out.contains("PB Header : Referer:PushBuilderAPIServlet"));

		assertFalse("FAIL found in response : " + out, out.contains("FAIL"));
	}

	private String getResponse(String uri, HashMap<String, String> headers) throws Exception {

		WebConversation wc = new WebConversation();
		String contextRoot = "/TestServlet40";

		StringBuilder url = new StringBuilder();

		url.append("http://");
		url.append(SHARED_SERVER.getLibertyServer().getHostname()); // trust
																	// Simplicity
																	// to
																	// provide
																	// host
		url.append(":");
		url.append(SHARED_SERVER.getLibertyServer().getHttpDefaultPort()); // trust
																			// Simplicity
																			// to
																			// provide
		url.append(contextRoot); // // port
		url.append(uri);

		Set<String> keys = headers.keySet();
		for (String key : keys) {
			wc.setHeaderField(key, headers.get(key));
		}

		LOG.info("getResponse: request url :" + url.toString());

		WebRequest request = new GetMethodWebRequest(url.toString());
		com.meterware.httpunit.WebResponse response = wc.getResponse(request);
		String text = response.getText();
		int code = response.getResponseCode();

		LOG.info("/*************************************************/");
		LOG.info("[WebContainer | testReaderFirst]: Return Code is: " + code);
		LOG.info("[WebContainer | testReaderFirst]: Response is: " + text);
		LOG.info("/*************************************************/");

		return text;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
	 */
	@Override
	protected SharedServer getSharedServer() {
		// TODO Auto-generated method stub
		return SHARED_SERVER;
	}

}
