/* =============================================================================
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
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
        if ( !JakartaEEAction.isEE9OrLaterActive()) {
            return;
        }

        for ( String config : EE9_TRANSFORMED_CONFIGS ) {
            Path configPath = Paths.get("lib/LibertyFATTestFiles", config);
            JakartaEEAction.transformApp(configPath);
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

    /**
     * Tests using different delivery delay values when sending messages to a Queue destination.
     * Runs tests with both simplified and domain-specific APIs
     * 
     * @throws Exception
     */
    @Test
    public void testDeliveryDelayForDifferentDelays_Queue() throws Exception {
    	
    	// Configure server to use MDB receiving from "local" Queue
        restartClient(MDB_CONFIG_QUEUE_BINDINGS);

        // Run test using simplified API
        runInServlet("testDeliveryDelayForDifferentDelays");

        String msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueBindingsMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);


        // Run the test using domain-specific API
        runInServlet("testDeliveryDelayForDifferentDelaysClassicApi");

        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueBindingsMessage-ClassicApi2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueBindingsMessage-ClassicApi1");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        
        
    	// Configure server to use MDB receiving from "remote" Queue
        restartServers(MDB_CONFIG_QUEUE_TCP);

        // Run test using simplified API
        runInServlet("testDeliveryDelayForDifferentDelays_Tcp");

        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset("Message received on mdb : QueueTCPMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        // Run test using domain-specific API
        runInServlet("testDeliveryDelayForDifferentDelaysClassicApi_Tcp");

        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueTCPMessage-ClassicApi2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : QueueTCPMessage-ClassicApi1");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        
        
        restartServers();
    }


    /**
     * Tests using different delivery delay values when sending messages to a Topic destination.
     * Runs tests with both simplified and domain-specific APIs
     * 
     * @throws Exception
     */
    @Test
    public void testDeliveryDelayForDifferentDelays_Topic() throws Exception {

    	// Configure server to use MDB receiving from "local" Topic
    	restartClient(MDB_CONFIG_TOPIC_BINDINGS);

    	// Run test using simplified API
        runInServlet("testDeliveryDelayForDifferentDelaysTopic");

        String msg = clientServer.waitForStringInLog("Message received on mdb : TopicBindingsMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLog("Message received on mdb : TopicBindingsMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        // Run test using domain-specific API
        runInServlet("testDeliveryDelayForDifferentDelaysTopicClassicApi");

        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicBindingsMessage-ClassicApi2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicBindingsMessage-ClassicApi1");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        
    	// Configure server to use MDB receiving from "remote" Topic
        restartServers(MDB_CONFIG_TOPIC_TCP);

        // Run test using simplified API
        runInServlet("testDeliveryDelayForDifferentDelaysTopic_Tcp");

        msg = clientServer.waitForStringInLog("Message received on mdb : TopicTCPMessage2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLog("Message received on mdb : TopicTCPMessage1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        // Run test using domain-specific API
        runInServlet("testDeliveryDelayForDifferentDelaysTopicClassicApi_Tcp");
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicTCPMessage-ClassicApi2");
        assertNotNull("Could not find the upload message in the trace.log", msg);
        msg = clientServer.waitForStringInLogUsingLastOffset(
            "Message received on mdb : TopicTCPMessage-ClassicApi1");
        assertNotNull("Could not find the upload message in the trace.log", msg);

        restartServers();
    }

    // Tests that test the persistence of delivery delay messages over a server restart
    // Each test calls a sender method in the servlet that sends 2 message, 1 persistent and 1 nonpersistent.
    // Then the servers are restarted and the servlet is called to attempt to receive the messages.
    // Where possible, these are amalgamated to call multiple sender and the associated receiver methods around a single server restart to reduce the number of restarts that are needed.
    // Note: for now the ptp messages are checked first, and if a failure is detected, the test fails at this point before the pub/sub results are checked. This could be improved later to check all the results and potentially give a richer response.

    // Simplified API tests
    @Test
    public void testPersistentMessageStore_B() throws Exception {
    	
    	// Send PtP messages
        runInServlet("testPersistentMessage");
        
        // Send pub/sub messages
        runInServlet("testPersistentMessageTopic");

        restartServers();

        // Receive ptp messages
        boolean ptpTestResult = runInServlet("testPersistentMessageReceive");
        assertTrue("testPersistentMessageStore_B failed", ptpTestResult);
        
        // Receive pubsub messages
        boolean pubsubTestResult = runInServlet("testPersistentMessageReceiveTopic");
        assertTrue("testPersistentMessageStoreTopic_B failed", pubsubTestResult);
    }


    @Test
    public void testPersistentMessageStore_Tcp() throws Exception {
    	
    	// Send PtP messages
        runInServlet("testPersistentMessage_Tcp");

        // Send pub/sub messages
        runInServlet("testPersistentMessageTopic_Tcp");

        restartServers();

        // Receive ptp messages
        boolean ptpTestResult = runInServlet("testPersistentMessageReceive_Tcp");
        assertTrue("testPersistentMessageStore_Tcp failed", ptpTestResult);
        
        // Receive pubsub messages
        boolean pubsubTestResult = runInServlet("testPersistentMessageReceiveTopic_Tcp");
        assertTrue("testPersistentMessageStoreTopic_B failed", pubsubTestResult);
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
    
    private String getServerFeature() {
        return ( JakartaEEAction.isEE9OrLaterActive() ? "messagingServer-3.0" : "wasJmsServer-1.0" );
    }

    private String getServerMessageFragment() {
        return ( JakartaEEAction.isEE9OrLaterActive() ? "messagingServer" : "wasJmsServer" );
    }

    private void verifyRemovedFeature(LibertyServer server, String fragment) throws Exception {
    	//CWWKF0013I: The server removed the following features: [wasJmsServer-1.0].
        String changedMessageFromLog = server.waitForStringInLogUsingMark("CWWKF0013I.*" + fragment + ".*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the \"CWWKF0013I:.*"+fragment+"\" feature removed message in the trace file",changedMessageFromLog);

        verifyFeatureUpdate(server);
    }

    private void verifyAddedFeature(LibertyServer server, String fragment) throws Exception {
    	// CWWKF0012I: The server installed the following features: [wasJmsServer-1.0].
        String changedMessageFromLog = server.waitForStringInLogUsingMark("CWWKF0012I.*" + fragment + ".*", server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the \"CWWKF0012I:.*"+fragment+"\" feature added message in the trace file",changedMessageFromLog);

        verifyFeatureUpdate(server);
        
        // Also wait for the jms server to restart
        // CWSID0108I: JMS server has started.
        String jmsServerStartedMessageFromLog = server.waitForStringInLogUsingMark("CWSID0108I.*",server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the \"CWSID0108I: JMS server has started.\"message in the trace file",jmsServerStartedMessageFromLog);
    }

    private void verifyFeatureUpdate(LibertyServer server) throws Exception {
    	//CWWKF0008I: Feature update completed in ?.??? seconds.
        String changedMessageFromLog = server.waitForStringInLogUsingMark("CWWKF0008I.*",server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the CWWKF0008I feature update completed message in the trace file", changedMessageFromLog);
    }
    
    /**
     * Look for the message:
     * "CWSID0108I: JMS server has started"
     * in the log to make sure that not only have feature updates completed, but also that the JMS provider is available.
     * @param server
     * @throws Exception
     */
    private void verifyJMSServerStarted(LibertyServer server) throws Exception {
    	//CWWKF0008I: Feature update completed in ?.??? seconds.
        String changedMessageFromLog = server.waitForStringInLogUsingMark("CWSID0108I.*",server.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the CWSID0108I 'JMS server has started' message in the trace file", changedMessageFromLog);
    	return;
    }


    
    //
    /**
     * <ul>
     * <li>Put a message to the client's messaging engine with a delivery delay.
     * <li>Remove and then restore the messaging feature.
     * <li>Wait for the message to be delivered.
     * <li>Check that the message appeared after the delivery delay even though the messaging 
     * feature was not installed for part of the delivery delay interval.
     * </ul>
     */
	/*
	 * @Test public void testDDRemoveAddServerFeature() throws Exception { try {
	 * boolean testResult1 = runInServlet("testSendMessage");
	 * assertTrue("testSendMessage failed", testResult1);
	 * 
	 * Set<String> clientFeatures =
	 * clientServer.getServerConfiguration().getFeatureManager().getFeatures();
	 * String serverFeature = getServerFeature(); String serverFragment =
	 * getServerMessageFragment();
	 * 
	 * clientServer.setMarkToEndOfLog(clientServer.getMatchingLogFile("trace.log"));
	 * clientFeatures.remove(serverFeature); clientServer.changeFeatures(new
	 * ArrayList<String>(clientFeatures)); verifyRemovedFeature(clientServer,
	 * serverFragment);
	 * 
	 * clientServer.setMarkToEndOfLog(clientServer.getMatchingLogFile("trace.log"));
	 * clientFeatures.add(serverFeature); clientServer.changeFeatures(new
	 * ArrayList<String>(clientFeatures)); verifyAddedFeature(clientServer,
	 * serverFragment);
	 * 
	 * // Wait until the JMS Server is actually running again. // There might still
	 * be a possibility that even in this case, the messaging singleton objects
	 * might not be available, but that's an issue to fix // elsewhere. Hopefully
	 * for the moment this will alleviate the problem with calling the app before
	 * the appropriate objects are available. verifyJMSServerStarted(clientServer);
	 * 
	 * 
	 * boolean testResult2 = runInServlet("testReceiveMessage");
	 * assertTrue("testReceiveMessage failed", testResult2);
	 * 
	 * } catch (Throwable throwable) { clientServer.serverDump(); throw throwable; }
	 * }
	 */
//    TODO
//      This test is disabled. After the jms server feature has been added back into the configuration and  
//      CWSID0108I: JMS server has started. has been written to the console, the JMS server has indeed been restarted.
//      The comms inbound chains will also have been restarted and will be listening using the current configuration.
//      However, the channel framework will not have been restarted and will be using the previous thread pool using the
//      same classloader that previously loaded SingletonsReady. that SingletonsReady no longer contains the Singletons
//      and results in the following FFDC.
//      
//      Exception = com.ibm.ws.messaging.lifecycle.LifecycleError
//      Source = com.ibm.ws.messaging.lifecycle.SingletonsReady
//      probeid = findService-LifecycleError
//      Stack Dump = com.ibm.ws.messaging.lifecycle.LifecycleError: Singletons are not yet ready. Examine the call stack for a service component where a dependency on SingletonsReady can be declared to resolve this error.
//	      at com.ibm.ws.messaging.lifecycle.SingletonsReady.requireService(SingletonsReady.java:172)
//	      at com.ibm.ws.messaging.lifecycle.SingletonsReady.findService(SingletonsReady.java:191)
//	      at com.ibm.ws.sib.common.service.CommonServiceFacade.getJsAdminService(CommonServiceFacade.java:65)
//	      at com.ibm.ws.jfap.inbound.channel.CommsServerServiceFacade.getJsAdminService(CommsServerServiceFacade.java:261)
//	      at com.ibm.ws.sib.trm.attach.TrmSingleton.handShake(TrmSingleton.java:135)
//	      at com.ibm.ws.sib.comms.server.clientsupport.ServerTransportReceiveListener.rcvTRMExchange(ServerTransportReceiveListener.java:1255)
//	      at com.ibm.ws.sib.comms.server.clientsupport.ServerTransportReceiveListener.dataReceived(ServerTransportReceiveListener.java:174)
//	      at com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ConversationReceiveListenerDataReceivedInvocation.invoke(ConversationReceiveListenerDataReceivedInvocation.java:170)
//	      at com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ReceiveListenerDispatchQueue.run(ReceiveListenerDispatchQueue.java:451)
//	      at com.ibm.ws.util.ThreadPool$Worker.run(ThreadPool.java:1671)
          
    // @Test
	/*
	 * public void testDDRemoveAddServerFeature_TCP() throws Exception { try {
	 * boolean testResult1 = runInServlet("testSendMessage_TCP");
	 * assertTrue("testSendMessage_TCP failed", testResult1);
	 * 
	 * Set<String> engineFeatures =
	 * engineServer.getServerConfiguration().getFeatureManager().getFeatures();
	 * String serverFeature = getServerFeature(); String serverFragment =
	 * getServerMessageFragment();
	 * 
	 * engineServer.setMarkToEndOfLog(engineServer.getMatchingLogFile("trace.log"));
	 * engineFeatures.remove(serverFeature); engineServer.changeFeatures(new
	 * ArrayList<String>(engineFeatures)); verifyRemovedFeature(engineServer,
	 * serverFragment);
	 * 
	 * engineServer.setMarkToEndOfLog(engineServer.getMatchingLogFile("trace.log"));
	 * engineFeatures.add(serverFeature); engineServer.changeFeatures(new
	 * ArrayList<String>(engineFeatures)); verifyAddedFeature(engineServer,
	 * serverFragment);
	 * 
	 * boolean testResult2 = runInServlet("testReceiveMessage_TCP");
	 * assertTrue("testReceiveMessage_TCP failed", testResult2);
	 * 
	 * } catch (Throwable throwable) { clientServer.serverDump();
	 * engineServer.serverDump(); throw throwable; } }
	 */}
