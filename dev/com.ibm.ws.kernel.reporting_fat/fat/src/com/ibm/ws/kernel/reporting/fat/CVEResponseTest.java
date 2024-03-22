/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.kernel.reporting.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.kernel.reporting.fat.response.CVEReportingResponseEndpoints;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;

/*
 * These tests are run using two different servers.
 * APP_SERVER runs the app (CVEReporting feature is disabled for this server)
 * TEST_SERVER runs the tests (CVEReporting feature is enabled for this server)
 * 
 * The reason for this is that because the CVE Reporting feature runs in the kernel the app was not
 * starting quick enough for the endpoints to be available to POST the data.
 */

@RunWith(FATRunner.class)
public class CVEResponseTest extends FATServletClient {

	public static final String APP_SERVER_NAME = "com.ibm.ws.kernel.reporting.response.server";
	public static final String TEST_SERVER_NAME = "com.ibm.ws.kernel.reporting.response.test.server";
	public static final String APP_NAME = "CVEReportingResponseEndpoints";

	private static final String KEYSTORE_FILENAME = "key.p12";

	protected static final Class<?> c = CVEResponseTest.class;

	@Server(APP_SERVER_NAME)
	public static LibertyServer server;

	@Server(TEST_SERVER_NAME)
	public static LibertyServer testServer;

	/*
	 * You must set the following in the server.xml when calling
	 * useSecondaryHTTPPort()
	 *
	 * <httpEndpoint id="defaultHttpEndpoint" host="*"
	 * httpPort="${bvt.prop.HTTP_secondary}"
	 * httpsPort="${bvt.prop.HTTP_secondary.secure}"/>
	 */

	@BeforeClass
	public static void setup() throws Exception {

		WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
				.addPackage(CVEReportingResponseEndpoints.class.getPackage());
		ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);
		testServer.useSecondaryHTTPPort();

	}

	@After
	public void tearDown() throws Exception {

		try {
			if (server.isStarted()) {
				server.stopServer();
			}
		} finally {
			if (testServer.isStarted()) {
				testServer.stopServer();
			}
		}
	}

	public static void copyTrustStore(LibertyServer server, LibertyServer testServer) throws Exception {
		testServer.copyFileToLibertyServerRoot(server.getServerRoot() + "/resources/security/", "resources/security",
				KEYSTORE_FILENAME);
	}

	/*
	 * This test makes a call to the /getResponse after the CVE reporting feature
	 * runs
	 */
	@Test
	public void testConnectionAndDataRecorded() throws Exception {
		server.startServer();
		server.waitForSSLStart();
		copyTrustStore(server, testServer);
		testServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dset.reporting.to.working=true",
				"-Djavax.net.ssl.trustStore=" + testServer.getServerRoot() + "/resources/security/key.p12",
				"-Djavax.net.ssl.trustStorePassword=password", "-Djavax.net.ssl.trustStoreType=PKCS12"));
		testServer.startServer();
		assertNotNull("CVE Reporting checks not been carried out", testServer.waitForStringInLog("CWWKF1703I"));

		server.waitForStringInLog("POST COMPLETED");

		JsonObject json = new HttpRequest(server, "/CVEReportingResponseEndpoints/endpoints/getResponse")
				.run(JsonObject.class);
//
		List<String> features = new ArrayList<String>();

		for (int i = 0; i < json.getJsonArray("features").size(); i++) {
			features.add(json.getJsonArray("features").getString(i));
		}

		String featuresAsString = Arrays.toString(features.toArray());

		assertTrue("The property 'id' is not null or empty",
				json.getString("id") != null && !json.getString("id").isEmpty());

		assertEquals("The property 'productEdition' did not match", "Open", json.getString("productEdition"));

		assertTrue("The property 'productVersion' did not match at the start",
				json.getString("productVersion").matches("^\\d\\d\\..*"));

		assertTrue("The property 'productVersion' did not match at the end",
				json.getString("productVersion").matches("^\\d\\d.0.0.([1-9]|1[0123])$"));

		String javaVendor = System.getProperty("java.vendor").toLowerCase();

		if (javaVendor == null) {
			javaVendor = System.getProperty("java.vm.name", "unknown").toLowerCase();
		}

		assertEquals("The property 'javaVendor' did not match", javaVendor, json.getString("javaVendor"));

		assertEquals("The property 'javaVersion' did not match", System.getProperty("java.runtime.version"),
				json.getString("javaVersion"));

		assertTrue("The property 'features' are incorrect", featuresAsString.contains("componenttest-")
				&& featuresAsString.contains("jsonb-") && featuresAsString.contains("mpRestClient-"));

		assertTrue("The property 'iFixes' is not empty", json.getJsonArray("iFixes").isEmpty());

	}

	/**
	 * Testing that if a server that does not exist is provided there will be no
	 * connection made and the correct warning message will be outputted to the logs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonExistingServer() throws Exception {
		ServerConfiguration config = testServer.getServerConfiguration();
		config.getCVEReporting().setUrlLink("https://localhost:65535/does/not/exist");
		testServer.updateServerConfiguration(config);
		testServer.startServer();

		assertNotNull("A connection has been made", testServer.waitForStringInLog("CWWKF1705W:.*"));

		testServer.stopServer("CWWKF1705");

	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIncorrectProtocol() throws Exception {
		ServerConfiguration config = testServer.getServerConfiguration();
		config.getCVEReporting().setUrlLink("http://localhost:65535/does/not/exist");
		testServer.updateServerConfiguration(config);
		testServer.startServer();

		assertNotNull("The MalformedURLException has not been thrown", testServer.waitForStringInLog("CWWKF1704W:.*"));

		testServer.stopServer("CWWKF1704");

	}
}
