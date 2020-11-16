/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DelayFullSecOffTest {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("DeliveryDelayEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("DeliveryDelayClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String appName = "DeliveryDelay";
    private static final String[] appPackages = new String[] { "deliverydelay.web" };
    private static final String contextRoot = "DeliveryDelay";

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, contextRoot, test); // throws IOException
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        engineServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");
        engineServer.setServerConfigurationFile("DelayEngine.xml");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");
        clientServer.setServerConfigurationFile("DelayClient.xml");
        TestUtils.addDropinsWebApp(clientServer, appName, appPackages);

        engineServer.startServer("DelayFullOff_Engine.log");
        clientServer.startServer("DelayFullOff_Client.log");
    }

    @AfterClass
    public static void tearDown() {
        try {
            clientServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        try {
            engineServer.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        ShrinkHelper.cleanAllExportedArchives();
    }

    //

    @Test
    public void testDeliveryDelayZeroAndNegativeValues_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValues");
        assertTrue("testDeliveryDelayZeroAndNegativeValues_B failed", testResult);
    }
    
    @Test
    public void testDeliveryDelayZeroAndNegativeValues_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValues_Tcp");
        assertTrue("testDeliveryDelayZeroAndNegativeValues_Tcp failed", testResult);
    }
    
    @Test
    public void testDeliveryDelayZeroAndNegativeValuesTopic_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopic");
        assertTrue("testDeliveryDelayZeroAndNegativeValuesTopic_B failed", testResult);
    }
    
    @AllowedFFDC({"com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"})
    @Test
    public void testDeliveryDelayZeroAndNegativeValuesTopic_Tcp()
        throws Exception {

        clientServer.stopServer();
        engineServer.stopServer();
        
        engineServer.setServerConfigurationFile("DelayEngine_Debug.xml");
        engineServer.startServer("DelayFullOff_Engine.log");

        String changedMessageFromLog = engineServer.waitForStringInLog("CWWKF0011I.*", engineServer.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find server ready message",changedMessageFromLog);
        
        clientServer.setServerConfigurationFile("DelayClient_Debug.xml");
        clientServer.startServer("DelayFullOff_Client.log");

        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopic_Tcp");
        assertTrue("testDeliveryDelayZeroAndNegativeValuesTopic_Tcp failed", testResult);
        
        clientServer.stopServer();
        engineServer.stopServer();

        engineServer.setServerConfigurationFile("DelayEngine.xml");
        engineServer.startServer("DelayFullOff_Engine.log");

        clientServer.setServerConfigurationFile("DelayClient.xml");
        clientServer.startServer("DelayFullOff_Client.log");
    }
    
    @Test
    public void testDeliveryMultipleMsgs_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgs");
        assertTrue("testDeliveryMultipleMsgs failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgs_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgs_Tcp");
        assertTrue("testDeliveryMultipleMsgs_Tcp failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgsTopic_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsTopic");
        assertTrue("testDeliveryMultipleMsgsTopic_B failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgsTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsTopic_Tcp");
        assertTrue("testDeliveryMultipleMsgsTopic_Tcp failed", testResult);
    }
    
    @Test
    public void testSettingMultipleProperties_B() throws Exception {
        boolean testResult = runInServlet("testSettingMultipleProperties");
        assertTrue("testSettingMultipleProperties_B failed", testResult);
    }
    
    @Test
    public void testSettingMultipleProperties_Tcp() throws Exception {
        boolean testResult = runInServlet("testSettingMultipleProperties_Tcp");
        assertTrue("testSettingMultipleProperties_Tcp failed", testResult);
    }
    
    @Test
    public void testSettingMultiplePropertiesTopic_B() throws Exception {
        boolean testResult = runInServlet("testSettingMultiplePropertiesTopic");
        assertTrue("testSettingMultiplePropertiesTopic_B failed", testResult);
    }
    
    @Test
    public void testSettingMultiplePropertiesTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testSettingMultiplePropertiesTopic_Tcp");
        assertTrue("testSettingMultiplePropertiesTopic_Tcp failed", testResult);
    }
    
    @Test
    public void testTransactedSend_B() throws Exception {
        boolean testResult = runInServlet("testTransactedSend_B");
        assertTrue("testTransactedSend_B failed", testResult);
    }
    
    @Test
    public void testTransactedSend_Tcp() throws Exception {
        boolean testResult = runInServlet("testTransactedSend_Tcp");
        assertTrue("testTransactedSend_Tcp failed", testResult);
    }
    
    @Test
    public void testTransactedSendTopic_B() throws Exception {
        boolean testResult = runInServlet("testTransactedSendTopic_B");
        assertTrue("testTransactedSendTopic_B failed", testResult);
    }
    
    @Test
    public void testTransactedSendTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testTransactedSendTopic_Tcp");
        assertTrue("testTransactedSendTopic_Tcp failed", testResult);
    }
    
    @Test
    public void testTiming_B() throws Exception {
        boolean testResult = runInServlet("testTiming_B");
        assertTrue("testTiming_B failed", testResult);
    }
    
    @Test
    public void testTiming_Tcp() throws Exception {
        boolean testResult = runInServlet("testTiming_Tcp");
        assertTrue("testTiming_Tcp failed", testResult);
    }
    
    @Test
    public void testTimingTopic_B() throws Exception {
        boolean testResult = runInServlet("testTimingTopic_B");
        assertTrue("testTimingTopic_B failed", testResult);
    }
    
    @Test
    public void testTimingTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimingTopic_Tcp");
        assertTrue("testTimingTopic_Tcp failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelay_B() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelay");
        assertTrue("testGetDeliveryDelay_B failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelay_Tcp() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelay_Tcp");
        assertTrue("testGetDeliveryDelay_Tcp failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelayTopic_B() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayTopic");
        assertTrue("testGetDeliveryDelayTopic_B failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelayTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayTopic_Tcp");
        assertTrue("testGetDeliveryDelayTopic_Tcp failed", testResult);
    }

    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelay_() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelay");
        assertTrue("testTimeToLiveWithDeliveryDelay failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelay_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelay_Tcp");
        assertTrue("testTimeToLiveWithDeliveryDelay_Tcp failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelayTopic_B() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelayTopic");
        assertTrue("testTimeToLiveWithDeliveryDelayTopic failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelayTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelayTopic_Tcp");
        assertTrue("testTimeToLiveWithDeliveryDelayTopic_Tcp failed", testResult);
    }
    
    // regression test for 118077 Queues
    
    @Test
    public void testReceiveBodyObjectMsgWithDD() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyObjectMsgWithDD");
        assertTrue("testReceiveBodyObjectMsgWithDD_ failed", testResult);
    }
    
    // regression test for 118077 Queues

    @Test
    public void testReceiveBodyObjectMsgWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testReceiveBodyObjectMsgWithDD_Tcp");
        assertTrue("testReceiveBodyObjectMsgWithDD_Tcp failed", testResult);
    }
    
    // regression test for 118076
    
    @Test
    public void testCloseConsumer() throws Exception {
        boolean testResult = runInServlet("testCloseConsumer");
        assertTrue("testCloseConsumer failed", testResult);
    }
    
    // regression test for 118076
    
    @Test
    public void testCloseConsumer_Tcp() throws Exception {
        boolean testResult = runInServlet("testCloseConsumer_Tcp");
        assertTrue("testCloseConsumer_Tcp failed", testResult);
    }
    
    // regression tests for 118067
    
    @Test
    public void testQueueNameNullWithDD() throws Exception {
        boolean testResult = runInServlet("testQueueNameNullWithDD");
        assertTrue("testQueueNameNullWithDD failed", testResult);
    }
    
    @Test
    public void testQueueNameNullWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testQueueNameNullWithDD_Tcp");
        assertTrue("testQueueNameNullWithDD_Tcp failed", testResult);
    }
    
    @Test
    public void testTopicNameNullWithDD() throws Exception {
        boolean testResult = runInServlet("testTopicNameNullWithDD");
        assertTrue("testTopicNameNullWithDD failed", testResult);
    }
    
    @Test
    public void testTopicNameNullWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testTopicNameNullWithDD_Tcp");
        assertTrue("testTopicNameNullWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 118070
    
    @Test
    public void testAckOnClosedContextWithDD() throws Exception {
        boolean testResult = runInServlet("testAckOnClosedContextWithDD");
        assertTrue("testAckOnClosedContextWithDD failed", testResult);
    }
    
    @Test
    public void testAckOnClosedContextWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testAckOnClosedContextWithDD_Tcp");
        assertTrue("testAckOnClosedContextWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 118075
    
    @Test
    public void testCreateConsumerWithMsgSelectorWithDD() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorWithDD");
        assertTrue("testCreateConsumerWithMsgSelectorWithDD failed", testResult);
    }
    
    @Test
    public void testCreateConsumerWithMsgSelectorWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorWithDD_Tcp");
        assertTrue("testCreateConsumerWithMsgSelectorWithDD_Tcp failed", testResult);
    }
    
    @Test
    public void testCreateConsumerWithMsgSelectorWithDDTopic() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorWithDDTopic");
        assertTrue("testCreateConsumerWithMsgSelectorWithDDTopic failed", testResult);
    }
    
    @Test
    public void testCreateConsumerWithMsgSelectorWithDDTopic_Tcp() throws Exception {
        boolean testResult = runInServlet("testCreateConsumerWithMsgSelectorWithDDTopic_Tcp");
        assertTrue("testCreateConsumerWithMsgSelectorWithDDTopic_Tcp failed", testResult);
    }
    
    // regression tests for 118061
    
    @Test
    public void testJMSPriorityWithDD() throws Exception {
        boolean testResult = runInServlet("testJMSPriorityWithDD");
        assertTrue("Test testJMSPriorityWithDD failed", testResult);
    }
    
    @Test
    public void testJMSPriorityWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testJMSPriorityWithDD_Tcp");
        assertTrue("Test testJMSPriorityWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 118058
    
    @Test
    public void testConnStartAuto_createContextUserSessionModeWithDD() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_createContextUserSessionModeWithDD");
        assertTrue("Test testConnStartAuto_createContextUserSessionModeWithDD failed", testResult);
    }
    
    @Test
    public void testConnStartAuto_createContextUserSessionModeWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testConnStartAuto_createContextUserSessionModeWithDD_Tcp");
        assertTrue("Test testConnStartAuto_createContextUserSessionModeWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 118062
    
    @Test
    public void testcreateBrowserWithDD() throws Exception {
        boolean testResult = runInServlet("testcreateBrowserWithDD");
        assertTrue("Test testcreateBrowserWithDD failed", testResult);
    }
    
    @Test
    public void testcreateBrowserWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testcreateBrowserWithDD_Tcp");
        assertTrue("Test testcreateBrowserWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 120846
    
    @Test
    public void testInitialJMSXDeliveryCountWithDD() throws Exception {
        boolean testResult = runInServlet("testInitialJMSXDeliveryCountWithDD");
        assertTrue("testInitialJMSXDeliveryCountWithDD failed", testResult);
    }
    
    @Test
    public void testInitialJMSXDeliveryCountWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testInitialJMSXDeliveryCountWithDD_Tcp");
        assertTrue("testInitialJMSXDeliveryCountWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 118071
    
    @Test
    public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic()throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic");
        assertTrue("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic failed", testResult);
    }
    
    @Test
    public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp() throws Exception {
        boolean testResult = runInServlet("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp");
        assertTrue("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp failed", testResult);
    }
    
    // regression tests for 118073
    
    @Test
    public void testClearProperties_NotsetWithDD() throws Exception {
        boolean testResult = runInServlet("testClearProperties_NotsetWithDD");
        assertTrue("Test testClearProperties_NotsetWithDD failed", testResult);
    }
    
    @Test
    public void testClearProperties_NotsetWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testClearProperties_NotsetWithDD_Tcp");
        assertTrue("Test testClearProperties_NotsetWithDD_Tcp failed", testResult);
    }
    
    @Test
    public void testStartJMSContextWithDD() throws Exception {
        boolean testResult = runInServlet("testStartJMSContextWithDD");
        assertTrue("testStartJMSContextWithDD failed", testResult);
    }
    
    @Test
    public void testStartJMSContextWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testStartJMSContextWithDD_Tcp");
        assertTrue("testStartJMSContextWithDD_Tcp failed", testResult);
    }
    
    // regression tests for 118068
    
    @Test
    public void testPTPTemporaryQueueWithDD() throws Exception {
        boolean testResult = runInServlet("testPTPTemporaryQueueWithDD");
        assertTrue("testPTPTemporaryQueueWithDD failed", testResult);
    }
    
    @Test
    public void testPTPTemporaryQueueWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testPTPTemporaryQueueWithDD_Tcp");
        assertTrue("testPTPTemporaryQueueWithDD_Tcp failed", testResult);
    }
    
    @Test
    public void testTemporaryTopicPubSubWithDD() throws Exception {
        boolean testResult = runInServlet("testTemporaryTopicPubSubWithDD");
        assertTrue("testTemporaryTopicPubSubWithDD failed", testResult);
    }
    
    @Test
    public void testTemporaryTopicPubSubWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testTemporaryTopicPubSubWithDD_Tcp");
        assertTrue("testTemporaryTopicPubSubWithDD_Tcp failed", testResult);
    }
    
    // regresion tests for 118065
    
    @Test
    public void testCommitLocalTransactionWithDD() throws Exception {
        boolean testResult = runInServlet("testCommitLocalTransactionWithDD");
        assertTrue("testCommitLocalTransactionWithDD failed", testResult);
    }
    
    @Test
    public void testCommitLocalTransactionWithDD_Tcp() throws Exception {
        boolean testResult = runInServlet("testCommitLocalTransactionWithDD_Tcp");
        assertTrue("testCommitLocalTransactionWithDD_Tcp failed", testResult);
    }
    
    @Test
    public void testDeliveryDelayZeroAndNegativeValuesClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValuesClassicApi");
        assertTrue("testDeliveryDelayZeroAndNegativeValuesClassicApi_B failed", testResult);
    }
    
    @Test
    public void testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp");
        assertTrue("testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopicClassicApi");
        assertTrue("testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp");
        assertTrue("testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgsClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsClassicApi");
        assertTrue("testDeliveryMultipleMsgsClassicApi failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgsClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsClassicApi_Tcp");
        assertTrue("testDeliveryMultipleMsgsClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgsTopicClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsTopicClassicApi");
        assertTrue("testDeliveryMultipleMsgsTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testDeliveryMultipleMsgsTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testDeliveryMultipleMsgsTopicClassicApi_Tcp");
        assertTrue("testDeliveryMultipleMsgsTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testSettingMultiplePropertiesClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testSettingMultiplePropertiesClassicApi");
        assertTrue("testSettingMultiplePropertiesClassicApi_B failed", testResult);
    }
    
    @Test
    public void testSettingMultiplePropertiesClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testSettingMultiplePropertiesClassicApi_Tcp");
        assertTrue("testSettingMultiplePropertiesClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testSettingMultiplePropertiesTopicClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testSettingMultiplePropertiesTopicClassicApi");
        assertTrue("testSettingMultiplePropertiesTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testSettingMultiplePropertiesTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testSettingMultiplePropertiesTopicClassicApi_Tcp");
        assertTrue("testSettingMultiplePropertiesTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testTransactedSendClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testTransactedSendClassicApi_B");
        assertTrue("testTransactedSendClassicApi_B failed", testResult);
    }
    
    @Test
    public void testTransactedSendClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testTransactedSendClassicApi_Tcp");
        assertTrue("testTransactedSendClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testTransactedSendTopicClassicApi_B() throws Exception {
        boolean testResult = runInServlet("testTransactedSendTopicClassicApi_B");
        assertTrue("testTransactedSendTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testTransactedSendTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testTransactedSendTopicClassicApi_Tcp");
        assertTrue("testTransactedSendTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testTimingClassicApi() throws Exception {
        boolean testResult = runInServlet("testTimingClassicApi_B");
        assertTrue("testTimingClassicApi_B failed", testResult);
    }
    
    @Test
    public void testTimingClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimingClassicApi_Tcp");
        assertTrue("testTimingClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testTimingTopicClassicApi() throws Exception {
        boolean testResult = runInServlet("testTimingTopicClassicApi_B");
        assertTrue("testTimingTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testTimingTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimingTopicClassicApi_Tcp");
        assertTrue("testTimingTopicClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelayClassicApi() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayClassicApi");
        assertTrue("testGetDeliveryDelayClassicApi_B failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelayClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayClassicApi_Tcp");
        assertTrue("testGetDeliveryDelayClassicApi_Tcp failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelayTopicClassicApi() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayClassicApiTopic");
        assertTrue("testGetDeliveryDelayTopicClassicApi_B failed", testResult);
    }
    
    @Test
    public void testGetDeliveryDelayTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testGetDeliveryDelayClassicApiTopic_Tcp");
        assertTrue("testGetDeliveryDelayTopicClassicApi_Tcp failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelayClassicApi() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelayClassicApi");
        assertTrue("testTimeToLiveWithDeliveryDelayClassicApi failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelayClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelayClassicApi_Tcp");
        assertTrue("testTimeToLiveWithDeliveryDelayClassicApi_Tcp failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelayTopicClassicApi() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelayTopicClassicApi");
        assertTrue("testTimeToLiveWithDeliveryDelayTopicClassicApi failed", testResult);
    }
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testTimeToLiveWithDeliveryDelayTopicClassicApi_Tcp() throws Exception {
        boolean testResult = runInServlet("testTimeToLiveWithDeliveryDelayTopicClassicApi_Tcp");
        assertTrue("testTimeToLiveWithDeliveryDelayTopicClassicApi_Tcp failed", testResult);
    }
    
    // regression tests for alias destinations

    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testJSAD_Send_Message() throws Exception {
        clientServer.setServerConfigurationFile("DelayClient_Alias.xml");

        boolean testResult1 = runInServlet("testJSAD_Send_Message_P2PTest");
        boolean testResult2 = runInServlet("testJSAD_Receive_Message_P2PTest");
        
        clientServer.setServerConfigurationFile("DelayClient.xml");

        assertTrue( "testJSAD_Send_Message failed", (testResult1 && testResult2) );
    }
    
    // regression tests for apiTD
    
    @Test
    public void testBasicTemporaryQueue() throws Exception {
        boolean testResult = runInServlet("testBasicTemporaryQueue");
        assertTrue("testBasicTemporaryQueue failed", testResult);
    }
    
    // regression tests for modifyMEResources
    
    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testP2PMessageInExceptionDestination() throws Exception {
        clientServer.stopServer();
        clientServer.setServerConfigurationFile("DelayClient_1.xml");
        clientServer.startServer("DelayFullOff_Client.log");

        boolean testResult1 = runInServlet("testSendMessageToQueue");

        clientServer.setServerConfigurationFile("DelayClient_2.xml");

        boolean testResult2 = runInServlet("testReadMsgFromExceptionQueue");

        clientServer.stopServer();
        clientServer.setServerConfigurationFile("DelayClient.xml");
        clientServer.startServer("DelayFullOff_Client.log");

        assertTrue( "testP2PMessageInExceptionDestination failed", (testResult1 && testResult2) );
    }
    
    // regression tests for SIM

    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testBytesMessage() throws Exception {
        boolean testResult = runInServlet("testBytesMessage");
        assertTrue("testBytesMessage failed", testResult);
    }

    // regression tests for Comms

    // @Test TODO: Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testComms_Send_Message_P2PTest_Default() throws Exception {
        boolean testResult = runInServlet("testComms_Send_Message_P2PTest_Default");
        assertTrue("testComms_Send_Message_P2PTest_Default failed", testResult);
    }
}
