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
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class DeliveryDelayFullModeTest {

	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("DeliveryDelayFullModeTest_ClientServer");

	private static LibertyServer server1 = LibertyServerFactory
			.getLibertyServer("DeliveryDelayFullModeTest_MEServer");

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
			e.printStackTrace();
		}
	}

	@Test
	public void testDeliveryDelayForDifferentDelays_B() throws Exception {

		server.stopServer();
		server.setServerConfigurationFile("QueueMDBClient_Bindings.xml");
		server.startServer();
		val = runInServlet("testDeliveryDelayForDifferentDelays");

		String msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage2");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server.setMarkToEndOfLog();
		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage1");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("QueueMDBClient_TCP.xml");
		server1.startServer();
		server.startServer();
		server.setMarkToEndOfLog();

		runInServlet("testDeliveryDelayForDifferentDelays_Tcp");

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage2");
		assertNotNull("Could not find the upload message in the trace.log", msg);
		server.setMarkToEndOfLog();
		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage1");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("JMSContext_Client.xml");
		server1.startServer();
		server.startServer();
	}

	@Test
	public void testDeliveryDelayForDifferentDelaysTopic_B() throws Exception {

		server.stopServer();
		server.setServerConfigurationFile("TopicMDBClient_Bindings.xml");
		server.startServer();
		String startMsg = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				startMsg);
		runInServlet("testDeliveryDelayForDifferentDelaysTopic");

		String msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicBindingsMessage2");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicBindingsMessage1");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("TopicMDBClient_TCP.xml");
		server1.startServer();
		server.startServer();

		String changedMessageFromLog = server1.waitForStringInLog(
				"CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);

		changedMessageFromLog = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);
		runInServlet("testDeliveryDelayForDifferentDelaysTopic_Tcp");

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicTCPMessage2");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicTCPMessage1");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("JMSContext_Client.xml");
		server1.startServer();
		server.startServer();

		changedMessageFromLog = server1.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);

		changedMessageFromLog = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);
	}

	@Test
	public void testPersistentMessageStore_B() throws Exception {

		val = runInServlet("testPersistentMessage");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceive");
		assertTrue("testPersistentMessageStore_B failed", val);

	}

	@Test
	public void testPersistentMessageStore_Tcp() throws Exception {

		val = runInServlet("testPersistentMessage_Tcp");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceive_Tcp");
		assertTrue("testPersistentMessageStore_Tcp failed", val);
	}

	@Test
	public void testPersistentMessageStoreTopic_B() throws Exception {

		val = runInServlet("testPersistentMessageTopic");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceiveTopic");
		assertTrue("testPersistentMessageStoreTopic_B failed", val);

	}

	@Test
	public void testPersistentMessageStoreTopic_Tcp() throws Exception {

		val = runInServlet("testPersistentMessageTopic_Tcp");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceiveTopic_Tcp");
		assertTrue("testPersistentMessageStoreTopic_B failed", val);
	}

	@Test
	public void testDeliveryDelayForDifferentDelaysClassicApi()
			throws Exception {

		server.stopServer();
		server.setServerConfigurationFile("QueueMDBClient_Bindings.xml");
		server.startServer();
		val = runInServlet("testDeliveryDelayForDifferentDelaysClassicApi");

		String msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage2-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server.setMarkToEndOfLog();
		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage1-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("QueueMDBClient_TCP.xml");
		server1.startServer();
		server.startServer();
		server.setMarkToEndOfLog();

		runInServlet("testDeliveryDelayForDifferentDelaysClassicApi_Tcp");

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage2-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);
		server.setMarkToEndOfLog();
		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage1-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("JMSContext_Client.xml");
		server1.startServer();
		server.startServer();
	}

	@Test
	public void testDeliveryDelayForDifferentDelaysTopicClassicApi()
			throws Exception {

		server.stopServer();
		server.setServerConfigurationFile("TopicMDBClient_Bindings.xml");
		server.startServer();
		String startMsg = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				startMsg);
		runInServlet("testDeliveryDelayForDifferentDelaysTopicClassicApi");

		String msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicBindingsMessage2-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicBindingsMessage1-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("TopicMDBClient_TCP.xml");
		server1.startServer();
		server.startServer();

		String changedMessageFromLog = server1.waitForStringInLog(
				"CWWKF0011I.*", server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);

		changedMessageFromLog = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);
		runInServlet("testDeliveryDelayForDifferentDelaysTopicClassicApi_Tcp");

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicTCPMessage2-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		msg = server
				.waitForStringInLogUsingLastOffset("Message received on mdb : TopicTCPMessage1-ClassicApi");
		assertNotNull("Could not find the upload message in the trace.log", msg);

		server1.stopServer();
		server.stopServer();
		server.setServerConfigurationFile("JMSContext_Client.xml");
		server1.startServer();
		server.startServer();

		changedMessageFromLog = server1.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);

		changedMessageFromLog = server.waitForStringInLog("CWWKF0011I.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);
	}

	@Test
	public void testPersistentMessageStoreClassicApi_B() throws Exception {

		val = runInServlet("testPersistentMessageClassicApi");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceiveClassicApi");
		assertTrue("testPersistentMessageStoreClassicApi_B failed", val);

	}

	@Test
	public void testPersistentMessageStoreClassicApi_Tcp() throws Exception {

		val = runInServlet("testPersistentMessageClassicApi_Tcp");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceiveClassicApi_Tcp");
		assertTrue("testPersistentMessageStoreClassicApi_Tcp failed", val);
	}

	@Test
	public void testPersistentMessageStoreTopicClassicApi_B() throws Exception {

		val = runInServlet("testPersistentMessageTopicClassicApi");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceiveTopicClassicApi");
		assertTrue("testPersistentMessageStoreTopicClassicApi_B failed", val);

	}

	@Test
	public void testPersistentMessageStoreTopicClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testPersistentMessageTopicClassicApi_Tcp");
		server.stopServer();
		server1.stopServer();
		server1.startServer();
		server.startServer();
		val = runInServlet("testPersistentMessageReceiveTopicClassicApi_Tcp");
		assertTrue("testPersistentMessageStoreTopicClassicApi_Tcp failed", val);
	}

	// regression tests for durable unshared

	@Test
	public void testCreateUnSharedDurable_B_SecOff() throws Exception {

		boolean val1 = false;
		boolean val2 = false;

		val1 = runInServlet("testCreateUnSharedDurableConsumer_create");
		server.stopServer();
		Thread.sleep(1000);
		server.startServer();

		val2 = runInServlet("testCreateUnSharedDurableConsumer_consume");

		if (val1 == true && val2 == true)
			val = true;

		assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", val);

	}

	@Test
	public void testCreateSharedDurable_B_SecOff() throws Exception {

		boolean val1 = false;
		boolean val2 = false;

		val1 = runInServlet("testCreateSharedDurableConsumer_create_B_SecOff");

		server.stopServer();
		Thread.sleep(1000);
		server.startServer("SharedSubscriptionTestClient_129623.log");

		val2 = runInServlet("testCreateSharedDurableConsumer_consume_B_SecOff");

		if (val1 == true && val2 == true)
			val = true;

		assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", val);

	}

	@Test
	public void testCreateSharedNonDurable_B_SecOff() throws Exception {

		boolean val1 = false;
		boolean val2 = false;
		val1 = runInServlet("testCreateSharedNonDurableConsumer_create_B_SecOff");

		server.stopServer();
		server.startServer("SharedSubscriptionTestClient_129626.log");

		val2 = runInServlet("testCreateSharedNonDurableConsumer_consume_B_SecOff");

		if (val1 == true && val2 == true)
			val = true;

		assertTrue("Test testCreateSharedNonDurable_B_SecOff failed", val);

	}

	@Test
	public void testDDRestartServer() throws Exception {

		val = runInServlet("testSendMessage");
		assertTrue("testSendMessage failed", val);

		server1.stopServer();
		server.stopServer();

		server1.startServer();
		String changedMessageFromLog = server1.waitForStringInLog(
				"CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);
		server.startServer();

		val = runInServlet("testReceiveMessage");
		assertTrue("testReceiveMessage failed", val);

	}

	@Test
	public void testDDRemoveAddServerFeature() throws Exception {

		val = runInServlet("testSendMessage");
		assertTrue("testSendMessage failed", val);

		List<String> featureList1 = new ArrayList<String>();

		featureList1.add("servlet-3.1");
		featureList1.add("jsp-2.3");
		featureList1.add("jndi-1.0");
		featureList1.add("testjmsinternals-1.0");
		featureList1.add("wasJmsClient-2.0");
		featureList1.add("osgiConsole-1.0");
		featureList1.add("timedexit-1.0");
		featureList1.add("el-3.0");

		server.changeFeatures(featureList1);
		server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
		// server.setServerConfigurationFile("JMSContext_ClientWithoutServerFeature.xml");
		String changedMessageFromLog = server.waitForStringInLogUsingMark(
				"CWWKF0013I.*wasJmsServer.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull(
				"Could not find the feature removed message in the trace file",
				changedMessageFromLog);

		featureList1.add("wasJmsServer-1.0");
		server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
		server.changeFeatures(featureList1);
		// server.setServerConfigurationFile("JMSContext_Client.xml");

		changedMessageFromLog = server.waitForStringInLogUsingMark(
				"CWWKF0012I.*wasJmsServer.*",
				server.getMatchingLogFile("trace.log"));
		assertNotNull(
				"Could not find the feature added message in the trace file",
				changedMessageFromLog);

		// changedMessageFromLog = server.waitForStringInLogUsingMark(
		// "CWWKT0016I.*DeliveryDelay.*",
		// server.getMatchingLogFile("trace.log"));
		// assertNotNull(
		// "Could not find the application ready message in the trace file",
		// changedMessageFromLog);

		int appCount = server.waitForMultipleStringsInLog(3,
				"CWWKT0016I.*DeliveryDelay.*");
		Log.info(DeliveryDelayFullModeTest.class, "CheckApplicationStart",
				"No. of times App started - " + appCount);
		assertTrue(
				"Could not find the application ready message in the log file",
				(appCount == 3));

		val = runInServlet("testReceiveMessage");
		assertTrue("testReceiveMessage failed", val);

	}

	@Test
	public void testDDRestartServer_TCP() throws Exception {

		val = runInServlet("testSendMessage_TCP");
		assertTrue("testSendMessage_TCP failed", val);

		server1.stopServer();
		server.stopServer();

		server1.startServer();
		String changedMessageFromLog = server1.waitForStringInLog(
				"CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
		assertNotNull("Could not find the upload message in the new file",
				changedMessageFromLog);
		server.startServer();

		val = runInServlet("testReceiveMessage_TCP");
		assertTrue("testReceiveMessage_TCP failed", val);

	}

	// @Test
	public void testDDRemoveAddServerFeature_TCP() throws Exception {

		val = runInServlet("testSendMessage_TCP");
		assertTrue("testSendMessage_TCP failed", val);
		server.stopServer();

		List<String> featureList1 = new ArrayList<String>();

		featureList1.add("servlet-3.1");
		featureList1.add("jsp-2.3");
		featureList1.add("jndi-1.0");
		featureList1.add("testjmsinternals-1.0");
		featureList1.add("wasJmsClient-2.0");
		featureList1.add("osgiConsole-1.0");
		featureList1.add("timedexit-1.0");
		featureList1.add("el-3.0");

		server1.changeFeatures(featureList1);
		server1.setMarkToEndOfLog(server1.getMatchingLogFile("trace.log"));
		// server.setServerConfigurationFile("JMSContext_ClientWithoutServerFeature.xml");
		String changedMessageFromLog = server1.waitForStringInLogUsingMark(
				"CWWKF0013I.*wasJmsServer.*",
				server1.getMatchingLogFile("trace.log"));
		assertNotNull(
				"Could not find the feature removed message in the trace file",
				changedMessageFromLog);

		featureList1.add("wasJmsServer-1.0");
		server1.setMarkToEndOfLog(server1.getMatchingLogFile("trace.log"));
		server1.changeFeatures(featureList1);
		// server.setServerConfigurationFile("JMSContext_Client.xml");

		changedMessageFromLog = server1.waitForStringInLogUsingMark(
				"CWWKF0012I.*wasJmsServer.*",
				server1.getMatchingLogFile("trace.log"));
		assertNotNull(
				"Could not find the feature added message in the trace file",
				changedMessageFromLog);
		changedMessageFromLog = server1.waitForStringInLogUsingMark(
				"CWWKF0008I.*", server1.getMatchingLogFile("trace.log"));
		assertNotNull(
				"Could not find the feature update completed message in the trace file",
				changedMessageFromLog);
		server.startServer();

		val = runInServlet("testReceiveMessage_TCP");
		assertTrue("testReceiveMessage_TCP failed", val);

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
