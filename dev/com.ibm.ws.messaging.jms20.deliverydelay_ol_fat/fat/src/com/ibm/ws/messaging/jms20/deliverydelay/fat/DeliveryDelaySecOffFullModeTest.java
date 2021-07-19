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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class DeliveryDelaySecOffFullModeTest {
	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("DeliveryDelaySecOffFullModeTest_ClientServer");

	private static LibertyServer server1 = LibertyServerFactory
			.getLibertyServer("DeliveryDelaySecOffFullModeTest_MEServer");

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
	public void testDeliveryDelayZeroAndNegativeValues_B() throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValues");
		assertTrue("testDeliveryDelayZeroAndNegativeValues_B failed", val);

	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValues_Tcp() throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValues_Tcp");
		assertTrue("testDeliveryDelayZeroAndNegativeValues_Tcp failed", val);
	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValuesTopic_B()
			throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopic");
		assertTrue("testDeliveryDelayZeroAndNegativeValuesTopic_B failed", val);

	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValuesTopic_Tcp()
			throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopic_Tcp");
		assertTrue("testDeliveryDelayZeroAndNegativeValuesTopic_Tcp failed",
				val);

	}

	@Test
	public void testDeliveryMultipleMsgs_B() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgs");
		assertTrue("testDeliveryMultipleMsgs failed", val);

	}

	@Test
	public void testDeliveryMultipleMsgs_Tcp() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgs_Tcp");
		assertTrue("testDeliveryMultipleMsgs_Tcp failed", val);
	}

	@Test
	public void testDeliveryMultipleMsgsTopic_B() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgsTopic");
		assertTrue("testDeliveryMultipleMsgsTopic_B failed", val);

	}

	@Test
	public void testDeliveryMultipleMsgsTopic_Tcp() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgsTopic_Tcp");
		assertTrue("testDeliveryMultipleMsgsTopic_Tcp failed", val);

	}

	@Test
	public void testSettingMultipleProperties_B() throws Exception {

		val = runInServlet("testSettingMultipleProperties");
		assertTrue("testSettingMultipleProperties_B failed", val);

	}

	@Test
	public void testSettingMultipleProperties_Tcp() throws Exception {

		val = runInServlet("testSettingMultipleProperties_Tcp");
		assertTrue("testSettingMultipleProperties_Tcp failed", val);

	}

	@Test
	public void testSettingMultiplePropertiesTopic_B() throws Exception {

		val = runInServlet("testSettingMultiplePropertiesTopic");
		assertTrue("testSettingMultiplePropertiesTopic_B failed", val);

	}

	@Test
	public void testSettingMultiplePropertiesTopic_Tcp() throws Exception {

		val = runInServlet("testSettingMultiplePropertiesTopic_Tcp");
		assertTrue("testSettingMultiplePropertiesTopic_Tcp failed", val);

	}

	@Test
	public void testTransactedSend_B() throws Exception {

		val = runInServlet("testTransactedSend_B");
		assertTrue("testTransactedSend_B failed", val);

	}

	@Test
	public void testTransactedSend_Tcp() throws Exception {

		val = runInServlet("testTransactedSend_Tcp");
		assertTrue("testTransactedSend_Tcp failed", val);

	}

	@Test
	public void testTransactedSendTopic_B() throws Exception {

		val = runInServlet("testTransactedSendTopic_B");
		assertTrue("testTransactedSendTopic_B failed", val);

	}

	@Test
	public void testTransactedSendTopic_Tcp() throws Exception {

		val = runInServlet("testTransactedSendTopic_Tcp");
		assertTrue("testTransactedSendTopic_Tcp failed", val);

	}

	@Test
	public void testTiming() throws Exception {

		val = runInServlet("testTiming_B");
		assertTrue("testTiming_B failed", val);

	}

	@Test
	public void testTiming_Tcp() throws Exception {

		val = runInServlet("testTiming_Tcp");
		assertTrue("testTiming_Tcp failed", val);

	}

	@Test
	public void testTimingTopic() throws Exception {

		val = runInServlet("testTimingTopic_B");
		assertTrue("testTimingTopic_B failed", val);

	}

	@Test
	public void testTimingTopic_Tcp() throws Exception {

		val = runInServlet("testTimingTopic_Tcp");
		assertTrue("testTimingTopic_Tcp failed", val);

	}

	@Test
	public void testGetDeliveryDelay() throws Exception {

		val = runInServlet("testGetDeliveryDelay");
		assertTrue("testGetDeliveryDelay_B failed", val);

	}

	@Test
	public void testGetDeliveryDelay_Tcp() throws Exception {

		val = runInServlet("testGetDeliveryDelay_Tcp");
		assertTrue("testGetDeliveryDelay_Tcp failed", val);

	}

	@Test
	public void testGetDeliveryDelayTopic() throws Exception {

		val = runInServlet("testGetDeliveryDelayTopic");
		assertTrue("testGetDeliveryDelayTopic_B failed", val);

	}

	@Test
	public void testGetDeliveryDelayTopic_Tcp() throws Exception {

		val = runInServlet("testGetDeliveryDelayTopic_Tcp");
		assertTrue("testGetDeliveryDelayTopic_Tcp failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelay() throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelay");
		assertTrue("testTimeToLiveWithDeliveryDelay failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelay_Tcp() throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelay_Tcp");
		assertTrue("testTimeToLiveWithDeliveryDelay_Tcp failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelayTopic() throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelayTopic");
		assertTrue("testTimeToLiveWithDeliveryDelayTopic failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelayTopic_Tcp() throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelayTopic_Tcp");
		assertTrue("testTimeToLiveWithDeliveryDelayTopic_Tcp failed", val);

	}

	// regression test for 118077 Queues

	@Test
	public void testReceiveBodyObjectMsgWithDD() throws Exception {

		val = runInServlet("testReceiveBodyObjectMsgWithDD");
		assertTrue("testReceiveBodyObjectMsgWithDD_ failed", val);

	}

	// regression test for 118077 Queues

	@Test
	public void testReceiveBodyObjectMsgWithDD_Tcp() throws Exception {

		val = runInServlet("testReceiveBodyObjectMsgWithDD_Tcp");
		assertTrue("testReceiveBodyObjectMsgWithDD_Tcp failed", val);

	}

	// regression test for 118076

	@Test
	public void testCloseConsumerWithDD() throws Exception {

		val = runInServlet("testCloseConsumer");
		assertTrue("testCloseConsumer_B_SecOff failed ", val);
	}

	// regression test for 118076

	@Test
	public void testCloseConsumerWithDD_Tcp() throws Exception {

		val = runInServlet("testCloseConsumer_Tcp");
		assertTrue("testCloseConsumerWithDD_Tcp failed ", val);

	}

	// regression tests for 118067

	@Test
	public void testQueueNameNullWithDD() throws Exception {

		val = runInServlet("testQueueNameNullWithDD");
		assertTrue("testQueueNameNullWithDD failed ", val);
	}

	@Test
	public void testQueueNameNullWithDD_Tcp() throws Exception {

		val = runInServlet("testQueueNameNullWithDD_Tcp");
		assertTrue("testQueueNameNullWithDD_Tcp failed ", val);
	}

	@Test
	public void testTopicNameNullWithDD() throws Exception {

		val = runInServlet("testTopicNameNullWithDD");
		assertTrue("testTopicNameNullWithDD failed ", val);

	}

	@Test
	public void testTopicNameNullWithDD_Tcp() throws Exception {

		val = runInServlet("testTopicNameNullWithDD_Tcp");
		assertTrue("testTopicNameNullWithDD_Tcp failed ", val);

	}

	// regression tests for 118070

	@Test
	public void testAckOnClosedContextWithDD() throws Exception {

		val = runInServlet("testAckOnClosedContextWithDD");
		assertTrue("testAckOnClosedContextWithDD failed ", val);

	}

	@Test
	public void testAckOnClosedContextWithDD_Tcp() throws Exception {

		val = runInServlet("testAckOnClosedContextWithDD_Tcp");
		assertTrue("testAckOnClosedContextWithDD_Tcp failed ", val);

	}

	// regression tests for 118075

	@Test
	public void testCreateConsumerWithMsgSelectorWithDD() throws Exception {

		val = runInServlet("testCreateConsumerWithMsgSelectorWithDD");
		assertTrue("testCreateConsumerWithMsgSelectorWithDD failed", val);

	}

	@Test
	public void testCreateConsumerWithMsgSelectorWithDD_Tcp() throws Exception {

		val = runInServlet("testCreateConsumerWithMsgSelectorWithDD_Tcp");
		assertTrue("testCreateConsumerWithMsgSelectorWithDD_Tcp failed", val);
	}

	@Test
	public void testCreateConsumerWithMsgSelectorWithDDTopic() throws Exception {

		val = runInServlet("testCreateConsumerWithMsgSelectorWithDDTopic");
		assertTrue("testCreateConsumerWithMsgSelectorWithDDTopic failed", val);

	}

	@Test
	public void testCreateConsumerWithMsgSelectorWithDDTopic_Tcp()
			throws Exception {

		val = runInServlet("testCreateConsumerWithMsgSelectorWithDDTopic_Tcp");
		assertTrue("testCreateConsumerWithMsgSelectorWithDDTopic_Tcp failed",
				val);
	}

	// regression tests for 118061

	@Test
	public void testJMSPriorityWithDD() throws Exception {

		val = runInServlet("testJMSPriorityWithDD");
		assertTrue("Test testJMSPriorityWithDD failed", val);

	}

	@Test
	public void testJMSPriorityWithDD_Tcp() throws Exception {

		val = runInServlet("testJMSPriorityWithDD_Tcp");
		assertTrue("Test testJMSPriorityWithDD_Tcp failed", val);

	}

	// regression tests for 118058

	@Test
	public void testConnStartAuto_createContextUserSessionModeWithDD()
			throws Exception {

		val = runInServlet("testConnStartAuto_createContextUserSessionModeWithDD");
		assertTrue(
				"Test testConnStartAuto_createContextUserSessionModeWithDD failed",
				val);

	}

	@Test
	public void testConnStartAuto_createContextUserSessionModeWithDD_Tcp()
			throws Exception {

		val = runInServlet("testConnStartAuto_createContextUserSessionModeWithDD_Tcp");
		assertTrue(
				"Test testConnStartAuto_createContextUserSessionModeWithDD_Tcp failed",
				val);
	}

	// regression tests for 118062

	@Test
	public void testcreateBrowserWithDD() throws Exception {

		val = runInServlet("testcreateBrowserWithDD");
		assertTrue("Test testcreateBrowserWithDD failed", val);

	}

	@Test
	public void testcreateBrowserWithDD_Tcp() throws Exception {

		val = runInServlet("testcreateBrowserWithDD_Tcp");
		assertTrue("Test testcreateBrowserWithDD_Tcp failed", val);
	}

	// regression tests for 120846

	@Test
	public void testInitialJMSXDeliveryCountWithDD() throws Exception {

		val = runInServlet("testInitialJMSXDeliveryCountWithDD");
		assertTrue("testInitialJMSXDeliveryCountWithDD failed", val);
	}

	@Test
	public void testInitialJMSXDeliveryCountWithDD_Tcp() throws Exception {

		val = runInServlet("testInitialJMSXDeliveryCountWithDD_Tcp");
		assertTrue("testInitialJMSXDeliveryCountWithDD_Tcp failed", val);

	}

	// regression tests for 118071

	@Test
	public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic()
			throws Exception {

		val = runInServlet("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic");
		assertTrue(
				"testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic failed",
				val);

	}

	@Test
	public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp()
			throws Exception {

		val = runInServlet("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp");
		assertTrue(
				"testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp failed",
				val);

	}

	// regression tests for 118073

	@Test
	public void testClearProperties_NotsetWithDD() throws Exception {

		val = runInServlet("testClearProperties_NotsetWithDD");
		assertTrue("Test testClearProperties_NotsetWithDD failed", val);

	}

	@Test
	public void testClearProperties_NotsetWithDD_Tcp() throws Exception {

		val = runInServlet("testClearProperties_NotsetWithDD_Tcp");
		assertTrue("Test testClearProperties_NotsetWithDD_Tcp failed", val);

	}

	@Test
	public void testStartJMSContextWithDD() throws Exception {

		val = runInServlet("testStartJMSContextWithDD");
		assertTrue("testStartJMSContextWithDD failed ", val);

	}

	@Test
	public void testStartJMSContextWithDD_Tcp() throws Exception {

		val = runInServlet("testStartJMSContextWithDD_Tcp");
		assertTrue("testStartJMSContextWithDD_Tcp failed ", val);

	}

	// regression tests for 118068

	@Test
	public void testPTPTemporaryQueueWithDD() throws Exception {

		val = runInServlet("testPTPTemporaryQueueWithDD");
		assertTrue("testPTPTemporaryQueueWithDD failed ", val);

	}

	@Test
	public void testPTPTemporaryQueueWithDD_Tcp() throws Exception {

		val = runInServlet("testPTPTemporaryQueueWithDD_Tcp");
		assertTrue("testPTPTemporaryQueueWithDD_Tcp failed ", val);
	}

	@Test
	public void testTemporaryTopicPubSubWithDD() throws Exception {

		val = runInServlet("testTemporaryTopicPubSubWithDD");
		assertTrue("testTemporaryTopicPubSubWithDD failed ", val);

	}

	@Test
	public void testTemporaryTopicPubSubWithDD_Tcp() throws Exception {

		val = runInServlet("testTemporaryTopicPubSubWithDD_Tcp");
		assertTrue("testTemporaryTopicPubSubWithDD_Tcp failed ", val);

	}

	// regresion tests for 118065

	@Test
	public void testCommitLocalTransactionWithDD() throws Exception {

		val = runInServlet("testCommitLocalTransactionWithDD");
		assertTrue("testCommitLocalTransactionWithDD failed ", val);

	}

	@Test
	public void testCommitLocalTransactionWithDD_Tcp() throws Exception {

		val = runInServlet("testCommitLocalTransactionWithDD_Tcp");
		assertTrue("testCommitLocalTransactionWithDD_Tcp failed ", val);

	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValuesClassicApi_B()
			throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValuesClassicApi");
		assertTrue("testDeliveryDelayZeroAndNegativeValuesClassicApi_B failed",
				val);

	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp");
		assertTrue(
				"testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp failed",
				val);
	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_B()
			throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopicClassicApi");
		assertTrue(
				"testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_B failed",
				val);

	}

	@Test
	public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp");
		assertTrue(
				"testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp failed",
				val);

	}

	@Test
	public void testDeliveryMultipleMsgsClassicApi_B() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgsClassicApi");
		assertTrue("testDeliveryMultipleMsgsClassicApi failed", val);

	}

	@Test
	public void testDeliveryMultipleMsgsClassicApi_Tcp() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgsClassicApi_Tcp");
		assertTrue("testDeliveryMultipleMsgsClassicApi_Tcp failed", val);
	}

	@Test
	public void testDeliveryMultipleMsgsTopicClassicApi_B() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgsTopicClassicApi");
		assertTrue("testDeliveryMultipleMsgsTopicClassicApi_B failed", val);

	}

	@Test
	public void testDeliveryMultipleMsgsTopicClassicApi_Tcp() throws Exception {

		val = runInServlet("testDeliveryMultipleMsgsTopicClassicApi_Tcp");
		assertTrue("testDeliveryMultipleMsgsTopicClassicApi_Tcp failed", val);

	}

	@Test
	public void testSettingMultiplePropertiesClassicApi_B() throws Exception {

		val = runInServlet("testSettingMultiplePropertiesClassicApi");
		assertTrue("testSettingMultiplePropertiesClassicApi_B failed", val);

	}

	@Test
	public void testSettingMultiplePropertiesClassicApi_Tcp() throws Exception {

		val = runInServlet("testSettingMultiplePropertiesClassicApi_Tcp");
		assertTrue("testSettingMultiplePropertiesClassicApi_Tcp failed", val);

	}

	@Test
	public void testSettingMultiplePropertiesTopicClassicApi_B()
			throws Exception {

		val = runInServlet("testSettingMultiplePropertiesTopicClassicApi");
		assertTrue("testSettingMultiplePropertiesTopicClassicApi_B failed", val);

	}

	@Test
	public void testSettingMultiplePropertiesTopicClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testSettingMultiplePropertiesTopicClassicApi_Tcp");
		assertTrue("testSettingMultiplePropertiesTopicClassicApi_Tcp failed",
				val);

	}

	@Test
	public void testTransactedSendClassicApi_B() throws Exception {

		val = runInServlet("testTransactedSendClassicApi_B");
		assertTrue("testTransactedSendClassicApi_B failed", val);

	}

	@Test
	public void testTransactedSendClassicApi_Tcp() throws Exception {

		val = runInServlet("testTransactedSendClassicApi_Tcp");
		assertTrue("testTransactedSendClassicApi_Tcp failed", val);

	}

	@Test
	public void testTransactedSendTopicClassicApi_B() throws Exception {

		val = runInServlet("testTransactedSendTopicClassicApi_B");
		assertTrue("testTransactedSendTopicClassicApi_B failed", val);

	}

	@Test
	public void testTransactedSendTopicClassicApi_Tcp() throws Exception {

		val = runInServlet("testTransactedSendTopicClassicApi_Tcp");
		assertTrue("testTransactedSendTopicClassicApi_Tcp failed", val);

	}

	@Test
	public void testTimingClassicApi() throws Exception {

		val = runInServlet("testTimingClassicApi_B");
		assertTrue("testTimingClassicApi_B failed", val);

	}

	@Test
	public void testTimingClassicApi_Tcp() throws Exception {

		val = runInServlet("testTimingClassicApi_Tcp");
		assertTrue("testTimingClassicApi_Tcp failed", val);

	}

	@Test
	public void testTimingTopicClassicApi() throws Exception {

		val = runInServlet("testTimingTopicClassicApi_B");
		assertTrue("testTimingTopicClassicApi_B failed", val);

	}

	@Test
	public void testTimingTopicClassicApi_Tcp() throws Exception {

		val = runInServlet("testTimingTopicClassicApi_Tcp");
		assertTrue("testTimingTopicClassicApi_Tcp failed", val);

	}

	@Test
	public void testGetDeliveryDelayClassicApi() throws Exception {

		val = runInServlet("testGetDeliveryDelayClassicApi");
		assertTrue("testGetDeliveryDelayClassicApi_B failed", val);

	}

	@Test
	public void testGetDeliveryDelayClassicApi_Tcp() throws Exception {

		val = runInServlet("testGetDeliveryDelayClassicApi_Tcp");
		assertTrue("testGetDeliveryDelayClassicApi_Tcp failed", val);

	}

	@Test
	public void testGetDeliveryDelayTopicClassicApi() throws Exception {

		val = runInServlet("testGetDeliveryDelayClassicApiTopic");
		assertTrue("testGetDeliveryDelayTopicClassicApi_B failed", val);

	}

	@Test
	public void testGetDeliveryDelayTopicClassicApi_Tcp() throws Exception {

		val = runInServlet("testGetDeliveryDelayClassicApiTopic_Tcp");
		assertTrue("testGetDeliveryDelayTopicClassicApi_Tcp failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelayClassicApi() throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelayClassicApi");
		assertTrue("testTimeToLiveWithDeliveryDelayClassicApi failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelayClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelayClassicApi_Tcp");
		assertTrue("testTimeToLiveWithDeliveryDelayClassicApi_Tcp failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelayTopicClassicApi()
			throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelayTopicClassicApi");
		assertTrue("testTimeToLiveWithDeliveryDelayTopicClassicApi failed", val);

	}

	// @Test
	public void testTimeToLiveWithDeliveryDelayTopicClassicApi_Tcp()
			throws Exception {

		val = runInServlet("testTimeToLiveWithDeliveryDelayTopicClassicApi_Tcp");
		assertTrue("testTimeToLiveWithDeliveryDelayTopicClassicApi_Tcp failed",
				val);

	}

	// regression tests for alias destinations
	// @Test
	public void testJSAD_Send_Message() throws Exception {

		server.setServerConfigurationFile("alias_server.xml");

		boolean val1 = runInServlet("testJSAD_Send_Message_P2PTest");
		boolean val2 = runInServlet("testJSAD_Receive_Message_P2PTest");

		if (val1 && val2)
			val = true;
		server.setServerConfigurationFile("JMSContext_Client.xml");
		assertTrue("testJSAD_Send_Message failed", val);

	}

	// regression tests for apiTD

	@Test
	public void testBasicTemporaryQueue() throws Exception {

		val = runInServlet("testBasicTemporaryQueue");
		assertTrue("testBasicTemporaryQueue failed", val);

	}

	// regression tests for modifyMEResources

	// @Test
	public void testP2PMessageInExceptionDestination() throws Exception {

		server.stopServer();
		server.setServerConfigurationFile("modifyME_1.xml");
		server.startServer();
		boolean val1 = runInServlet("testSendMessageToQueue");
		server.setServerConfigurationFile("modifyME_2.xml");
		boolean val2 = runInServlet("testReadMsgFromExceptionQueue");
		server.stopServer();
		server.setServerConfigurationFile("JMSContext_Client.xml");
		server.startServer();
		if (val1 && val2)
			val = true;

		assertTrue("testP2PMessageInExceptionDestination failed", val);

	}

	// regression tests for SIM

	// @Test
	public void testBytesMessage() throws Exception {

		val = runInServlet("testBytesMessage");
		assertTrue("testBytesMessage failed", val);

	}

	// regression tests for Comms

	// @Test
	public void testComms_Send_Message_P2PTest_Default() throws Exception {

		val = runInServlet("testComms_Send_Message_P2PTest_Default");
		assertTrue("testComms_Send_Message_P2PTest_Default failed", val);

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
