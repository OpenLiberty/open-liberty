/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class PropertySettingTest {

	// Server Information
	public static String server1Port = "8091";

	// Basic URL
	@Server("policyattachments_client")
	public static LibertyServer client;

	@Server("propertytest_server")
	public static LibertyServer server1;

	// Test URL
	// Client with URI and EndpointReference policy attachment configurations
	public static String clientApp1 = "policyAttachmentsClient1";

	// Service with URI policy attachment configuration
	public static String serviceApp1 = "policyAttachmentsService1";

	public static String ClientServlet1 = "ClientServlet1";
	public static String helloWithoutPolicy = "helloWithoutPolicy";
	public static String helloWithoutPolicyResult = "helloWithoutPolicy invoked";

	public static String errorResult = "WS-AT Feature is not installed";

	@BeforeClass
	public static void setup() throws Exception {

		WebArchive serviceWar1 = ShrinkWrap.create(WebArchive.class, serviceApp1 + ".war").addPackages(false,
				"com.ibm.ws.policyattachments.service1");

		WebArchive clientWar1 = ShrinkWrap.create(WebArchive.class, clientApp1 + ".war")
				.addPackages(false, "com.ibm.ws.policyattachments.client1")
				.addPackages(false, "com.ibm.ws.policyattachments.client1.service1")
				.addPackages(false, "com.ibm.ws.policyattachments.client1.service2")
				.addPackages(false, "com.ibm.ws.policyattachments.client1.service3");

		ShrinkHelper.addDirectory(serviceWar1, "test-applications/" + serviceApp1 + "/resources");
		ShrinkHelper.addDirectory(clientWar1, "test-applications/" + clientApp1 + "/resources");

		ShrinkHelper.exportDropinAppToServer(server1, serviceWar1);
		ShrinkHelper.exportDropinAppToServer(client, clientWar1);

		// Make sure we don't fail because we try to start an
		// already started server
		try {
			client.startServer();
			server1.startServer();
			assertNotNull("The server did not start", server1.waitForStringInLog("CWWKF0011I"));
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		if (server1 != null) {
			server1.stopServer();
		}
		if (client != null) {
			client.stopServer();
		}
	}

	/**
	 * Client with URI policy attachment, service with URI policy attachment
	 */
	@Test
	public void testCxfIgnoreUnsupportedPolicyProperty() {
		commonTest(clientApp1, ClientServlet1, serviceApp1, helloWithoutPolicy, helloWithoutPolicyResult);
		// Property test
		assertNotNull("Property cxf.ignore.unsupported.policy is failed to be enabled",
				server1.waitForStringInTrace("because property cxf.ignore.unsupported.policy is set to true"));
	}

	public static void commonTest(String clientName, String servletName, String serviceName, String testMethod,
			String expectResult) {

		String resultURL = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/" + clientName
				+ "/" + servletName + "?app=" + serviceName + "&method=" + testMethod;

		try {
			String result = executeApp(resultURL);
			assertTrue("Check result, expect is " + expectResult + ", result is " + result,
					expectResult.equals(result));

		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	public static String executeApp(String url) throws Exception {
		HttpURLConnection con = HttpUtils.getHttpConnection(new URL(url), HttpURLConnection.HTTP_OK, 60);
		BufferedReader br = HttpUtils.getConnectionStream(con);
		String result = br.readLine();
		System.out.println("Execute WS-AT Policy Attachment test from " + url);
		return result;
	}
}
