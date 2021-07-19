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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class DeliveryDelaySecOnLiteModeTest {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("DeliveryDelaySecOnLiteModeTest_ClientServer");

	private static LibertyServer server1 = LibertyServerFactory
			.getLibertyServer("DeliveryDelaySecOnLiteModeTest_MEServer");

	private static final int PORT = server.getHttpDefaultPort();
	// private static final int PORT = 9090;
	private static final String HOST = server.getHostname();

	boolean val;

	private static boolean runInServlet(String test) throws IOException {

		boolean result;

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
		server.startServer("JMSDeliveryDelayClient.log");
		server1.startServer("JMSDeliveryDelayServer.log");
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
	public void testSetDeliveryDelaySecOn_B() throws Exception {

		val = runInServlet("testSetDeliveryDelay");
		assertTrue("testSetDeliveryDelay_B failed", val);
	}

	@Test
	public void testSetDeliveryDelaySecOn_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelay_Tcp");
		assertTrue("testSetDeliveryDelay_Tcp failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicSecOn_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopic");
		assertTrue("testSetDeliveryDelayTopic_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicSecOn_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopic_Tcp");
		assertTrue("testSetDeliveryDelayTopic_Tcp failed", val);
	}

	@Test
	public void testSetDeliveryDelayTopicDurSubSecOn_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSub");
		assertTrue("testSetDeliveryDelayTopicDurSub_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicDurSubSecOn_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSub_Tcp");
		assertTrue("testSetDeliveryDelayTopicDurSub_Tcp failed", val);
	}

	@Test
	public void testSetDeliveryDelayClassicSecOn_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayClassicApi");
		assertTrue("testSetDeliveryDelayClassicApi_B failed", val);
	}

	@Test
	public void testSetDeliveryDelayClassicApiSecOn_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayClassicApi_Tcp");
		assertTrue("testSetDeliveryDelayClassicApi_Tcp failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicClassicApiSecOn_B() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicClassicApi");
		assertTrue("testSetDeliveryDelayTopicClassicApi_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicClassicApiSecOn_Tcp() throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicClassicApi_Tcp");
		assertTrue("testSetDeliveryDelayTopicClassicApi_Tcp failed", val);
	}

	@Test
	public void testSetDeliveryDelayTopicDurSubClassicApiSecOn_B()
			throws Exception {

		val = runInServlet("testSetDeliveryDelayTopicDurSubClassicApi");
		assertTrue("testSetDeliveryDelayTopicDurSubClassicApi_B failed", val);

	}

	@Test
	public void testSetDeliveryDelayTopicDurSubClassicApiSecOn_Tcp()
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
