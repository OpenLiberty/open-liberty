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

package com.ibm.ws.messaging.JMS20.fat.DurableUnshared;

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

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class DurableUnshared {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("DurableUnshared_TestServer");
	private static LibertyServer server1 = LibertyServerFactory
			.getLibertyServer("DurableUnshared_TestServer1");

	private static final int PORT = server.getHttpDefaultPort();
	private static final String HOST = server.getHostname();

	private static boolean val = false;

	private boolean runInServlet(String test) throws IOException {
		boolean result = false;

		URL url = new URL("http://" + HOST + ":" + PORT
				+ "/DurableUnshared?test=" + test);
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
		server.startServer("DurableUnShared_Client.log");
		server1.startServer("DurableUnShared_Server.log");

	}

	// 1st method
	// Bindings and Security Off

	@Test
	public void testCreateUnSharedDurable_B_SecOn() throws Exception {

		boolean val1 = false;
		boolean val2 = false;
		val1 = runInServlet("testCreateUnSharedDurableConsumer_create");

		server.stopServer();
		server.startServer();

		val2 = runInServlet("testCreateUnSharedDurableConsumer_consume");
		if (val1 == true && val2 == true)
			val = true;

		assertTrue("testCreateUnSharedDurable_B_SecOn failed", val);

	}

	// TCP and Security Off

	@Test
	public void testCreateUnSharedDurable_TCP_SecOn() throws Exception {

		boolean val1 = false;
		boolean val2 = false;
		val1 = runInServlet("testCreateUnSharedDurableConsumer_create_TCP");

		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();

		val2 = runInServlet("testCreateUnSharedDurableConsumer_consume_TCP");
		if (val1 == true && val2 == true)
			val = true;

		assertTrue("testCreateUnSharedDurable_TCP_SecOn failed", val);

	}

	// -----------------------------------------=================================================================

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

        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, JMSContextwar, OVERWRITE);
        Archive DurableUnsharedwar = ShrinkWrap.create(WebArchive.class, "DurableUnshared.war")
            .addClass("web.DurableUnsharedServlet")
            .add(new FileAsset(new File("test-applications//DurableUnshared.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//DurableUnshared.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, DurableUnsharedwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, DurableUnsharedwar, OVERWRITE);
    }
}
