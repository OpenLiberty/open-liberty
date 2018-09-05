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
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class DeliveryDelaySecOffLiteModeTest {

	@ClassRule
	public static final TestRule java7Rule = new OnlyRunInJava7Rule();

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("DeliveryDelaySecOffLiteModeTest_ClientServer");

	private static LibertyServer server1 = LibertyServerFactory
			.getLibertyServer("DeliveryDelaySecOffLiteModeTest_MEServer");

	private static final int PORT = server.getHttpDefaultPort();
	private static final String HOST = server.getHostname();

	boolean val = false;
	boolean val1 = false;
	boolean val2 = false;

	private boolean runInServlet(String test) throws IOException {

		boolean result = false;

		URL url = new URL("http://" + HOST + ":" + PORT
				+ "/DeliveryDelay?test=" + test);
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


		server.copyFileToLibertyInstallRoot("lib/features",
				"features/testjmsinternals-1.0.mf");
		server1.setServerConfigurationFile("JMSContext_Server.xml");
		server1.startServer();
		String changedMessageFromLog = server1.waitForStringInLog(
				"CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);

		server.setServerConfigurationFile("JMSContext_Client.xml");

		server.startServer();

		changedMessageFromLog = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);

	}

	@AfterClass
	public static void tearDown() {
		try {
			System.out.println("Stopping server");
			server.stopServer();
			server1.stopServer();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testSetDeliveryDelay_B() throws Exception {

		val = runInServlet("testSetDeliveryDelay");
		assertTrue("testSetDeliveryDelay_B failed", val);
	}

	@Test
	public void testSetDeliveryDelay_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelay_Tcp");
		assertTrue("testSetDeliveryDelay_Tcp failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopic_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopic");
		assertTrue("testSetDeliveryDelayTopic_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopic_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopic_Tcp");
		assertTrue("testSetDeliveryDelayTopic_Tcp failed", val);
	}

	@Test
	public void testSetDeliveryDelayTopicDurSub_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSub");
		assertTrue("testSetDeliveryDelayTopicDurSub_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicDurSub_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSub_Tcp");
		assertTrue("testSetDeliveryDelayTopicDurSub_Tcp failed", val);
	}

	@Test
	public void testSetDeliveryDelayClassic_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayClassicApi");
		assertTrue("testSetDeliveryDelayClassicApi_B failed", val);
	}

	@Test
	public void testSetDeliveryDelayClassicApi_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayClassicApi_Tcp");
		assertTrue("testSetDeliveryDelayClassicApi_Tcp failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicClassicApi_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicClassicApi");
		assertTrue("testSetDeliveryDelayTopicClassicApi_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicClassicApi_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicClassicApi_Tcp");
		assertTrue("testSetDeliveryDelayTopicClassicApi_Tcp failed", val);
	}

	@Test
	public void testSetDeliveryDelayTopicDurSubClassicApi_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi");
		assertTrue("testSetDeliveryDelayTopicDurSubClassicApi_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicDurSubClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi_Tcp");
		assertTrue("testSetDeliveryDelayTopicDurSubClassicApi_Tcp failed", val);
	}

    public static void setUpShirnkWrap() throws Exception {

        Archive DeliveryDelaywar = ShrinkWrap.create(WebArchive.class, "DeliveryDelay.war")
            .addClass("web.DeliveryDelayServlet")
            .add(new FileAsset(new File("test-applications//DeliveryDelay.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//DeliveryDelay.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, DeliveryDelaywar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, DeliveryDelaywar, OVERWRITE);
    }
}
