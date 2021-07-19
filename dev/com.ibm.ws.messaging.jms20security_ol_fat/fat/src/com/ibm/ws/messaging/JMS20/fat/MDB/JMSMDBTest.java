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
package com.ibm.ws.messaging.JMS20.fat.MDB;

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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSMDBTest {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("JMSMDBTest_TestServer");

	private static final int PORT = server.getHttpDefaultPort();
	private static final String HOST = server.getHostname();

	boolean val = false;

	private boolean runInServlet(String test) throws IOException {

		boolean result = false;
		URL url = new URL("http://" + HOST + ":" + PORT
				+ "/JMSContextInject?test=" + test);
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

			if (lines.indexOf(test + " COMPLETED SUCCESSFULLY") < 0) {
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
		server.copyFileToLibertyServerRoot("resources/security",
				"clientLTPAKeys/mykey.jks");
		server.setServerConfigurationFile("EJBMDB_server.xml");
		server.startServer("JMSConsumerTestClient.log");
	}

	@org.junit.AfterClass
	public static void tearDown() {
		try {
			System.out.println("Stopping server");
			server.stopServer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testQueueMDB() throws Exception {

		val = runInServlet("testQueueMDB");
		assertTrue("testQueueMDB failed ", val);

		String msg = server.waitForStringInLog(
				"Message received on Annotated MDB: testQueueMDB", 5000);
		assertNotNull("Test testQueueMDB failed", msg);

	}

    public static void setUpShirnkWrap() throws Exception {

        Archive JMSConsumerwar = ShrinkWrap.create(WebArchive.class, "JMSConsumer.war")
            .addClass("web.JMSConsumerServlet")
            .add(new FileAsset(new File("test-applications//JMSConsumer.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSConsumer.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSConsumerwar, OVERWRITE);
        Archive JMSContextInjectwar = ShrinkWrap.create(WebArchive.class, "JMSContextInject.war")
            .addClass("web.JMSContextInjectServlet")
            .add(new FileAsset(new File("test-applications//JMSContextInject.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContextInject.war/resources/WEB-INF/beans.xml")), "WEB-INF/beans.xml")
            .add(new FileAsset(new File("test-applications//JMSContextInject.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextInjectwar, OVERWRITE);
        Archive JMSContextwar = ShrinkWrap.create(WebArchive.class, "JMSContext.war")
            .addClass("web.JMSContextServlet")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
            .add(new FileAsset(new File("test-applications//JMSContext.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, JMSContextwar, OVERWRITE);
    }
}
