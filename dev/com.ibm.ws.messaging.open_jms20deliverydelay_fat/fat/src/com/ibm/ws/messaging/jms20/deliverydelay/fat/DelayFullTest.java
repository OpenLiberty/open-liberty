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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DelayFullTest {
    private static LibertyServer engineServer =
        LibertyServerFactory.getLibertyServer("DeliveryDelayEngine");

    private static LibertyServer clientServer =
        LibertyServerFactory.getLibertyServer("DeliveryDelayClient");
    private static final int clientPort = clientServer.getHttpDefaultPort();
    private static final String clientHost = clientServer.getHostname();

    private static final String ddAppName = "DeliveryDelay";
    private static final String[] ddAppPackages = new String[] { "deliverydelay.web" };
    private static final String ddContextRoot = "DeliveryDelay";

    private static final String mdbAppName = "jmsapp.ear";
    private static final String mdbJarName = "jmsmdb";
    private static final String[] mdbJarPackages = new String[] { "jmsmdb.ejb" };

    private static final String MDB_CONFIG = "DelayClient_MDB.xml";

    private static final String MDB_CONFIG_QUEUE_BINDINGS = "DelayClient_QueueMDB_Bindings.xml";
    private static final String MDB_CONFIG_QUEUE_TCP = "DelayClient_QueueMDB_TCP.xml";

    private static final String MDB_CONFIG_TOPIC_BINDINGS = "DelayClient_TopicMDB_Bindings.xml";
    private static final String MDB_CONFIG_TOPIC_TCP = "DelayClient_TopicMDB_TCP.xml";

    private static final String[] EE9_TRANSFORMED_CONFIGS = new String[] {
        MDB_CONFIG_TOPIC_BINDINGS,
        MDB_CONFIG_TOPIC_TCP
    };

    private static void transformConfigurations() throws Exception {
        if ( !JakartaEE9Action.isActive() ) {
            return;
        }

        for ( String config : EE9_TRANSFORMED_CONFIGS ) {
            Path configPath = Paths.get("lib/LibertyFATTestFiles", config);
            JakartaEE9Action.transformApp(configPath);
        }
    }

    private static final boolean THROW_EXCEPTIONS = true;

    private static void stopClient(boolean throwExceptions) throws Exception {
        if ( throwExceptions ) {
            clientServer.stopServer();
        } else {
            try {
                clientServer.stopServer();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }

    // This FAT suite requires that the client server be stopped before
    // the engine server is stopped, and requires that engine server be
    // started before the client server is started.

    private static void stopServers(boolean throwExceptions) throws Exception {
        if ( throwExceptions ) {
            clientServer.stopServer();
            engineServer.stopServer();
        } else {
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
        }
    }

    private static void startServers() throws Exception{
        startServers(DEFAULT_CLIENT);
    }

    private static final String DEFAULT_CLIENT = null;

    private static void startEngine() throws Exception {
        String engineConfiguration = "DelayEngine.xml";
        engineServer.setServerConfigurationFile(engineConfiguration);
        engineServer.startServer("DelayFull_Engine.log");
    }

    private static void startClient(String clientConfiguration) throws Exception {
        if ( clientConfiguration == DEFAULT_CLIENT ) {
            clientConfiguration = MDB_CONFIG;
        }
        clientServer.setServerConfigurationFile(clientConfiguration);
        clientServer.startServer("DelayFull_Client.log");
    }

    private static void startServers(String clientConfiguration) throws Exception {
        startEngine();
        startClient(clientConfiguration);
    }

    private static void restartClient() throws Exception {
        restartClient(DEFAULT_CLIENT);
    }

    private static void restartClient(String clientConfiguration) throws Exception {
        stopClient(THROW_EXCEPTIONS);
        startClient(clientConfiguration);
    }

    private static void restartServers(String clientConfiguration) throws Exception{
        stopServers(THROW_EXCEPTIONS);
        startServers(clientConfiguration);
    }

    private static void restartServers() throws Exception {
        stopServers(THROW_EXCEPTIONS);
        startServers();
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        transformConfigurations();

        engineServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");

        clientServer.copyFileToLibertyInstallRoot(
            "lib/features", "features/testjmsinternals-1.0.mf");

        TestUtils.addDropinsWebApp(clientServer, ddAppName, ddAppPackages);

        // Commercial liberty places the MDB EJB JAR in an application, and as a
        // configured application, not as a drop-ins application.
        //
        // Running the MDB EJB JAR as a drop-in JAR does not work.

        JavaArchive mdbJar = ShrinkHelper.buildJavaArchive(mdbJarName, mdbJarPackages);
        EnterpriseArchive mdbEar =  ShrinkWrap.create(EnterpriseArchive.class, mdbAppName);
        mdbEar.addAsModule(mdbJar);
        ShrinkHelper.exportToServer(clientServer, "apps", mdbEar);

        startServers();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServers(!THROW_EXCEPTIONS);

        ShrinkHelper.cleanAllExportedArchives();
    }

    private boolean runInServlet(String test) throws IOException {
        return TestUtils.runInServlet(clientHost, clientPort, ddContextRoot, test); // throws IOException
    }

    @Test
    public void testDeliveryDelayForDifferentDelays_B() throws Exception {
        restartClient(MDB_CONFIG_QUEUE_BINDINGS);

        runInServlet("testDeliveryDelayForDifferentDelays");

        String msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers(MDB_CONFIG_QUEUE_TCP);

        runInServlet("testDeliveryDelayForDifferentDelays_Tcp");

        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers();
    }

    @Test
    public void testDeliveryDelayForDifferentDelaysTopic_B() throws Exception {
        restartClient(MDB_CONFIG_TOPIC_BINDINGS);

        runInServlet("testDeliveryDelayForDifferentDelaysTopic");

        String msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : TopicBindingsMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : TopicBindingsMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers(MDB_CONFIG_TOPIC_TCP);

        runInServlet("testDeliveryDelayForDifferentDelaysTopic_Tcp");

        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : TopicTCPMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : TopicTCPMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers();
    }

    @Test
    public void testPersistentMessageStore_B() throws Exception {
        runInServlet("testPersistentMessage");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceive");
        assertTrue("testPersistentMessageStore_B failed", testResult);
    }

    @Test
    public void testPersistentMessageStore_Tcp() throws Exception {
        runInServlet("testPersistentMessage_Tcp");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceive_Tcp");
        assertTrue("testPersistentMessageStore_Tcp failed", testResult);
    }

    @Test
    public void testPersistentMessageStoreTopic_B() throws Exception {
        runInServlet("testPersistentMessageTopic");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceiveTopic");
        assertTrue("testPersistentMessageStoreTopic_B failed", testResult);
    }

    @Test
    public void testPersistentMessageStoreTopic_Tcp() throws Exception {
        runInServlet("testPersistentMessageTopic_Tcp");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceiveTopic_Tcp");
        assertTrue("testPersistentMessageStoreTopic_B failed", testResult);
    }

    @Test
    public void testDeliveryDelayForDifferentDelaysClassicApi() throws Exception {
        restartClient(MDB_CONFIG_QUEUE_BINDINGS);

        runInServlet("testDeliveryDelayForDifferentDelaysClassicApi");

        String msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueBindingsMessage2-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueBindingsMessage1-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers(MDB_CONFIG_QUEUE_TCP);

        runInServlet("testDeliveryDelayForDifferentDelaysClassicApi_Tcp");

        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueTCPMessage2-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueTCPMessage1-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers();
    }

    @Test
    public void testDeliveryDelayForDifferentDelaysTopicClassicApi()throws Exception {
        restartClient(MDB_CONFIG_TOPIC_BINDINGS);

        runInServlet("testDeliveryDelayForDifferentDelaysTopicClassicApi");

        String msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicBindingsMessage2-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicBindingsMessage1-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers(MDB_CONFIG_TOPIC_TCP);

        runInServlet("testDeliveryDelayForDifferentDelaysTopicClassicApi_Tcp");
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicTCPMessage2-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicTCPMessage1-ClassicApi");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers();
    }

    @Test
    public void testPersistentMessageStoreClassicApi_B() throws Exception {
        runInServlet("testPersistentMessageClassicApi");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceiveClassicApi");
        assertTrue("testPersistentMessageStoreClassicApi_B failed", testResult);
    }

    @Test
    public void testPersistentMessageStoreClassicApi_Tcp() throws Exception {
        runInServlet("testPersistentMessageClassicApi_Tcp");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceiveClassicApi_Tcp");
        assertTrue("testPersistentMessageStoreClassicApi_Tcp failed", testResult);
    }

    @Test
    public void testPersistentMessageStoreTopicClassicApi_B() throws Exception {
        runInServlet("testPersistentMessageTopicClassicApi");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceiveTopicClassicApi");
        assertTrue("testPersistentMessageStoreTopicClassicApi_B failed", testResult);
    }

    @Test
    public void testPersistentMessageStoreTopicClassicApi_Tcp()throws Exception {
        runInServlet("testPersistentMessageTopicClassicApi_Tcp");

        restartServers();

        boolean testResult = runInServlet("testPersistentMessageReceiveTopicClassicApi_Tcp");
        assertTrue("testPersistentMessageStoreTopicClassicApi_Tcp failed", testResult);
    }

    // regression tests for durable unshared

    @Test
    public void testCreateUnSharedDurable_B_SecOff() throws Exception {
        boolean testResult = true;

        if ( !runInServlet("testCreateUnSharedDurableConsumer_create") ) {
            testResult = false;
        }
        
        restartClient();

        if ( !runInServlet("testCreateUnSharedDurableConsumer_consume") ) {
            testResult = false;
        }

        assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateSharedDurable_B_SecOff() throws Exception {
        boolean testResult = true;

        if ( !runInServlet("testCreateSharedDurableConsumer_create_B_SecOff") ) {
            testResult = false;
        }

        restartClient();

        if ( !runInServlet("testCreateSharedDurableConsumer_consume_B_SecOff") ) {
            testResult = false;
        }

        assertTrue("testCreateSharedDurableExpiry_B_SecOff failed", testResult);
    }

    @Test
    public void testCreateSharedNonDurable_B_SecOff() throws Exception {
        boolean testResult = true;

        if ( !runInServlet("testCreateSharedNonDurableConsumer_create_B_SecOff") ) {
            testResult = false;
        }

        restartClient();

        if ( !runInServlet("testCreateSharedNonDurableConsumer_consume_B_SecOff") ) {
            testResult = false;
        }

        assertTrue("Test testCreateSharedNonDurable_B_SecOff failed", testResult);
    }

    @Test
    public void testDDRestartServer() throws Exception {
        boolean testResult1 = runInServlet("testSendMessage");
        assertTrue("testSendMessage failed", testResult1);

        restartServers();

        boolean testResult2 = runInServlet("testReceiveMessage");
        assertTrue("testReceiveMessage failed", testResult2);
    }

    @Test
    public void testDDRestartServer_TCP() throws Exception {
        boolean testResult1 = runInServlet("testSendMessage_TCP");
        assertTrue("testSendMessage_TCP failed", testResult1);

        restartServers();

        boolean testResult2 = runInServlet("testReceiveMessage_TCP");
        assertTrue("testReceiveMessage_TCP failed", testResult2);
    }

    //

    private List<String> getClientFeatures() {
        List<String> features = new ArrayList<String>();

        features.add("osgiconsole-1.0");
        features.add("jndi-1.0");

        if ( JakartaEE9Action.isActive() ) {
            features.add("servlet-5.0");
            features.add("pages-3.0");
            features.add("messagingClient-3.0");
            features.add( getServerFeature() );
        } else {
            features.add("servlet-3.1");
            features.add("jsp-2.3");
            features.add("wasJmsClient-2.0");
            features.add( getServerFeature() );
        }
        
        features.add("testjmsinternals-1.0");

        return features;
    }

    private String getServerFeature() {
        return ( JakartaEE9Action.isActive() ? "messagingServer-3.0" : "wasJmsServer-1.0" );
    }

    private String getServerMessageFragment() {
        return ( JakartaEE9Action.isActive() ? "messagingServer" : "wasJmsServer" );
    }

    private void verifyRemovedFeature(LibertyServer server, String fragment) throws Exception {
        String changedMessageFromLog = server.waitForStringInLogUsingMark(
            "CWWKF0013I.*" + fragment + ".*",
            server.getMatchingLogFile("trace.log"));
        assertNotNull(
            "Could not find the feature removed message in the trace file",
            changedMessageFromLog);

        verifyFeatureUpdate(server);
    }

    private void verifyAddedFeature(LibertyServer server, String fragment) throws Exception {
        String changedMessageFromLog = server.waitForStringInLogUsingMark(
            "CWWKF0012I.*" + fragment + ".*",
            server.getMatchingLogFile("trace.log"));
        assertNotNull(
            "Could not find the feature added message in the trace file",
            changedMessageFromLog);

        verifyFeatureUpdate(server);
    }

    private void verifyFeatureUpdate(LibertyServer server) throws Exception {
        String changedMessageFromLog = server.waitForStringInLogUsingMark(
            "CWWKF0008I.*",
            server.getMatchingLogFile("trace.log"));
        assertNotNull(
            "Could not find the feature update completed message in the trace file",
            changedMessageFromLog);
    }

    @Test
    public void testDDRemoveAddServerFeature() throws Exception {
        boolean testResult1 = runInServlet("testSendMessage");
        assertTrue("testSendMessage failed", testResult1);

        List<String> features = getClientFeatures();
        String serverFeature = getServerFeature();
        String serverFragment = getServerMessageFragment();

        clientServer.setMarkToEndOfLog(clientServer.getMatchingLogFile("trace.log"));
        features.remove(serverFeature);
        clientServer.changeFeatures(features);
        verifyRemovedFeature(clientServer, serverFragment);

        clientServer.setMarkToEndOfLog(clientServer.getMatchingLogFile("trace.log"));
        features.add(serverFeature);
        clientServer.changeFeatures(features);
        verifyAddedFeature(clientServer, serverFragment);

        int appCount = clientServer.waitForMultipleStringsInLog(3, "CWWKT0016I.*DeliveryDelay.*");
        Log.info(DelayFullTest.class, "CheckApplicationStart", "No. of times App started - " + appCount);
        assertTrue( "Could not find the application ready message in the log file", (appCount == 3) );

        boolean testResult2 = runInServlet("testReceiveMessage");
        assertTrue("testReceiveMessage failed", testResult2);
    }

    // @Test TODO Already disabled.
    //       Should be fixed or removed in a subsequent issue or pull-request.
    public void testDDRemoveAddServerFeature_TCP() throws Exception {
        boolean testResult1 = runInServlet("testSendMessage_TCP");
        assertTrue("testSendMessage_TCP failed", testResult1);

        List<String> features = getClientFeatures();
        String serverFeature = getServerFeature();
        String serverFragment = getServerMessageFragment();

        clientServer.setMarkToEndOfLog(clientServer.getMatchingLogFile("trace.log"));
        features.remove(serverFeature);
        clientServer.changeFeatures(features);
        verifyRemovedFeature(clientServer, serverFragment);

        clientServer.setMarkToEndOfLog(clientServer.getMatchingLogFile("trace.log"));
        features.add(serverFeature);
        clientServer.changeFeatures(features);
        verifyAddedFeature(clientServer, serverFragment);

        boolean testResult2 = runInServlet("testReceiveMessage_TCP");
        assertTrue("testReceiveMessage_TCP failed", testResult2);
    }
}
