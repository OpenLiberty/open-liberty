/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.TemporaryQueue;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.junit.BeforeClass;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import com.ibm.websphere.simplicity.ShrinkHelper;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSContextTest_118066 {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("JMSContextTest_118066_TestServer");
	private static LibertyServer server1 = LibertyServerFactory
			.getLibertyServer("JMSContextTest_118066_TestServer1");

	private static final int PORT = server.getHttpDefaultPort();
	private static final String HOST = server.getHostname();

	private static boolean val = false;

	private boolean runInServlet(String test) throws IOException {
		boolean result = false;
		URL url = new URL("http://" + HOST + ":" + PORT
				+ "/TemporaryQueue?test=" + test);
		System.out.println("The Servlet URL is : " + url.toString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		try {
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestMethod("GET");
			con.connect();

			InputStream is = con.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String sep = System.getProperty("line.separator");
			StringBuilder lines = new StringBuilder();
			for (String line = br.readLine(); line != null; line = br
					.readLine())
				lines.append(line).append(sep);

			if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
				org.junit.Assert.fail("Missing success message in output. "
						+ lines);

				result = false;
			} else
				result = true;

			return result;
		} finally {
			con.disconnect();
		}
	}

	@BeforeClass
	public static void testConfigFileChange() throws Exception {

            setUpShirnkWrap();

		server1.copyFileToLibertyInstallRoot("lib/features",
				"features/testjmsinternals-1.0.mf");
		server1.copyFileToLibertyServerRoot("resources/security",
				"serverLTPAKeys/cert.der");
		server1.copyFileToLibertyServerRoot("resources/security",
				"serverLTPAKeys/ltpa.keys");
		server1.copyFileToLibertyServerRoot("resources/security",
				"serverLTPAKeys/mykey.jks");

		server.copyFileToLibertyInstallRoot("lib/features",
				"features/testjmsinternals-1.0.mf");
		server.copyFileToLibertyServerRoot("resources/security",
				"clientLTPAKeys/mykey.jks");

		server.setServerConfigurationFile("JMSContext_ssl.xml");
		server1.setServerConfigurationFile("TestServer1_ssl.xml");
		server.startServer("JMSContextTest_118066_Client.log");
		server1.startServer("JMSContextTest_118066_Server.log");

	}

	// 118066_1 :
	// ----- 118066
	// This story is used to implement the functionality of Start and stop for
	// JMSContext
	// Below test cases are the APIs of Start and stop for JMSContext

	@Test
	public void testStartJMSContextSecOnBinding() throws Exception {

		val = runInServlet("testStartJMSContextSecOnBinding");
		assertTrue("testStartJMSContextSecOnBinding failed ", val);

	}

	@Test
	public void testStartJMSContextSecOnTCP() throws Exception {

		val = runInServlet("testStartJMSContextSecOnTCP");
		assertTrue("testStartJMSContextSecOnTCP failed ", val);

	}

	@Mode(TestMode.FULL)
	@Test
	public void testStartJMSContextStartSecOnBinding() throws Exception {

		val = runInServlet("testStartJMSContextStartSecOnBinding");
		assertTrue("testStartJMSContextStartSecOnBinding failed ", val);
	}

	@Mode(TestMode.FULL)
	@Test
	public void testStartJMSContextStartSecOnTCP() throws Exception {

		val = runInServlet("testStartJMSContextStartSecOnTCP");
		assertTrue("testStartJMSContextStartSecOnTCP failed ", val);
	}

	@Test
	public void testStopJMSContextSecOnBinding() throws Exception {

		val = runInServlet("testStopJMSContextSecOnBinding");
		assertTrue("testStopJMSContextSecOnBinding failed ", val);

	}

	@Test
	public void testStopJMSContextSecOnTCPIP() throws Exception {

		val = runInServlet("testStopJMSContextSecOnTCP");
		assertTrue("testStopJMSContextSecOnTCPIP failed ", val);
	}

	@org.junit.AfterClass
	public static void tearDown1() {
		try {
			System.out.println("Stopping server");
			server1.stopServer();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@org.junit.AfterClass
	public static void tearDown2() {
		try {
			System.out.println("Stopping server");
			server.stopServer();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    public static void setUpShirnkWrap() throws Exception {

        Archive TemporaryQueuewar = ShrinkWrap.create(WebArchive.class, "TemporaryQueue.war")
            .addClass("web.JMSContextTestServlet")
            .add(new FileAsset(new File("test-applications//TemporaryQueue.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//TemporaryQueue.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, TemporaryQueuewar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, TemporaryQueuewar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
    }
}
