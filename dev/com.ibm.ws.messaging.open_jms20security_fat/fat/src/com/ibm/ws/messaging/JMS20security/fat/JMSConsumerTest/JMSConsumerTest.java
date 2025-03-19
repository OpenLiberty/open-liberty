/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20security.fat.JMSConsumerTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.JMSActivationSpec;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.config.WasJmsProperties;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.messaging.JMS20security.fat.TestUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JMSConsumerTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");
    private static final LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static final Class<?> c = JMSConsumerTest.class;
    
    //static { Logger.getLogger(c.getName()).setLevel(Level.FINER);}
    // Output goes to .../com.ibm.ws.messaging.open_jms20security_fat/build/libs/autoFVT/results/output.txt

    /** @return the methodName of the caller. */
    private final static String methodName() { return new Exception().getStackTrace()[1].getMethodName(); }
    
    private static boolean runInServlet(String test) throws IOException {

        boolean result;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSConsumer?test="
                          + test);
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
            String sep = System.lineSeparator();
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
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

        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/cert.der");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/ltpa.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                              "serverLTPAKeys/ltpaFIPS.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/mykey.jks");
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.copyFileToLibertyServerRoot("resources/security",
                                           "clientLTPAKeys/mykey.jks");
        
        // Add the client servlet application to the client appserver.
        TestUtils.addDropinsWebApp(server, "JMSConsumer", "web");
        // Add the client side jmsConsumer.mdb's to the client appserver. 
        TestUtils.addDropinsWebApp(server, "jmsapp", "jmsConsumer.mdb");
        
        transformServerXml("JMSContext_ssl.xml");
        
        startAppServers();
    }

    /**
     * Update a Server.xml so that it uses appropriate javax or jakarta queue and topic classes in ActivationSpecs.
     */
    private static void transformServerXml(String fileName) throws Exception {
        File file = new File("lib/LibertyFATTestFiles/"+fileName);
        Log.info(c, "transformServerXml", "Updating ActivationSpec destinationType in:" + file);
        ServerConfiguration config = ServerConfigurationFactory.fromFile(file);

        Map<String, String> transformation = new HashMap<>();
        if (JakartaEEAction.isEE9OrLaterActive()) {
            transformation.put("javax.jms.Queue", "jakarta.jms.Queue");
            transformation.put("javax.jms.Topic", "jakarta.jms.Topic");
        } else {
            transformation.put("jakarta.jms.Queue", "javax.jms.Queue");
            transformation.put("jakarta.jms.Topic", "javax.jms.Topic"); 
        }
        
        // Update JMS queue and topic destinationType.
        boolean configUpdated = false;
        for (JMSActivationSpec jmsActivationSpec : config.getJMSActivationSpecs()) {
            for (WasJmsProperties properties : jmsActivationSpec.getWasJmsProperties()) {
                String destinationType = properties.getDestinationType();
                if (destinationType != null && transformation.containsKey(destinationType)) {
                    properties.setDestinationType(transformation.get(destinationType));
                    configUpdated = true;
                }
            }
        }

        if (configUpdated) {
            ServerConfigurationFactory.toFile(file, config);
            Log.info(c, "transformServerXml", "Updated " + file);
        } else {
            Log.info(c, "transformServerXml", "No changes to " + file);
        }
    }

    /**
     * Start both the JMSConsumerClient local and remote messaging engine AppServers.
     *
     * @throws Exception
     */
    private static void startAppServers() throws Exception {
        startAppServers( "JMSContext_ssl.xml", "TestServer1_ssl.xml");
    }
    
    private static void startAppServers(String clientConfigFile, String remoteConfigFile) throws Exception {
        server.setServerConfigurationFile(clientConfigFile);
        server1.setServerConfigurationFile(remoteConfigFile);
        // Start the remote server first to increase the odds of the client making contact at the first attempt.
        server1.startServer("JMSConsumerServer.log");
        server.startServer("JMSConsumerTestClient.log");
     
        // CWWKF0011I: The TestServer1 server is ready to run a smarter planet. The TestServer1 server started in 6.435 seconds.
        // CWSID0108I: JMS server has started.
        // CWWKS4105I: LTPA configuration is ready after 4.028 seconds.
        for (String messageId : new String[] { "CWWKF0011I.*", "CWSID0108I.*", "CWWKS4105I.*" }) {
            String waitFor = server.waitForStringInLog(messageId, server.getMatchingLogFile("messages.log"));
            assertNotNull("Server message " + messageId + " not found", waitFor);
            waitFor = server1.waitForStringInLog(messageId, server1.getMatchingLogFile("messages.log"));
            assertNotNull("Server1 message " + messageId + " not found", waitFor);
        }
        
        // Wait for CWSIV0556I: Connection to the Messaging Engine was successful. The message-driven bean with activation specification jmsapp/RDC2MessageDrivenBean will now be able to receive the messages from destination RedeliveryQueue1.
        String waitFor = server.waitForStringInLog("CWSIV0556I:.*jmsapp/RDC2MessageDrivenBean.*", server.getMatchingLogFile("messages.log"));
        assertNotNull("Client Server contact remote server1 message CWSIV0556I: not found", waitFor);
        
        // The following FFDC may be thrown at server startup because the channel framework does not become active until the CWWKF0011I message is seen, whereas MDB initialisation takes place beforehand.
        // FFDC1015I: An FFDC Incident has been created: "com.ibm.wsspi.channelfw.exception.InvalidChainNameException: Chain configuration not found in framework, BootstrapSecureMessaging com.ibm.ws.sib.jfapchannel.richclient.framework.impl.RichClientTransportFactory.getOutboundNetworkConnectionFactoryByName 00280001" at ffdc_21.09.27_15.21.46.0.log
        
        // Ignore failed connection attempts between the two servers.
        // CWSIV0782W: The creation of a connection for destination RedeliveryQueue1 on bus defaultBus for endpoint activation jmsapp/jmsmdb/RDC2MessageDrivenBean failed with exception javax.resource.ResourceException: 
        server.addIgnoredErrors(Arrays.asList("CWSIV0782W"));
    }

    private static void stopAppServers() throws Exception {
        server.stopServer();
        server1.stopServer();  
    }
    
    
    // start 118076
    @Test
    public void testCloseConsumer_B_SecOn() throws Exception {

        boolean val = runInServlet("testCloseConsumer_B");
        assertTrue("testCloseConsumer_B_SecOn failed", val);

    }

    // TCP and Security on ( with ssl)

    @Test
    public void testCloseConsumer_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testCloseConsumer_TCP");
        assertTrue("testCloseConsumer_TCP_SecOn failed", val);

    }

    // end 118076

    // start 118077
    @Test
    public void testReceive_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceive_B");
        assertTrue("testReceive_B_SecOn failed", val);
    }

    @Test
    public void testReceive_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testReceive_TCP");
        assertTrue("testReceive_TCP_SecOn failed", val);

    }

    @Test
    public void testReceiveBody_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBody_B");
        assertTrue("testReceiveBody_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBody_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBody_TCP");
        assertTrue("testReceiveBody_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOut_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyTimeOut_B");
        assertTrue("testReceiveBodyTimeOut_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOut_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyTimeOut_TCP");
        assertTrue("testReceiveBodyTimeOut_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveBodyNoWait_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWait_B");
        assertTrue("testReceiveBodyNoWait_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWait_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWait_TCP");
        assertTrue("testReceiveBodyNoWait_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveWithTimeOut_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveWithTimeOut_B_SecOn");
        assertTrue("testReceiveWithTimeOut_B_SecOn failed", val);
    }

    @Test
    public void testReceiveWithTimeOut_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveWithTimeOut_TcpIp_SecOn");
        assertTrue("testReceiveWithTimeOut_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveNoWait_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveNoWait_B_SecOn");
        assertTrue("testReceiveNoWait_B_SecOn failed", val);
    }

    @Test
    public void testReceiveNoWait_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveNoWait_TcpIp_SecOn");
        assertTrue("testReceiveNoWait_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveBodyEmptyBody_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyEmptyBody_B_SecOn");
        assertTrue("testReceiveBodyEmptyBody_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyEmptyBody_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyEmptyBody_B_SecOn");
        assertTrue("testReceiveBodyEmptyBody_TcpIp_SecOn failed", val);
    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn");
        assertTrue("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn");
        assertTrue(
                   "testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedType_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWaitUnsupportedType_B_SecOn");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn");
        assertTrue("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveTopic_B");
        assertTrue("testReceiveTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveTopic_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveTopic_TCP");
        assertTrue("testReceiveTopic_TCP_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyTopic_B");
        assertTrue("testReceiveBodyTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyTopic_TCP");
        assertTrue("testReceiveBodyTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOutTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyTimeOutTopic_B");
        assertTrue("testReceiveBodyTimeOutTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyTimeOutTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyTimeOutTopic_TCP");
        assertTrue("testReceiveBodyTimeOutTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWaitTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWaitTopic_B");
        assertTrue("testReceiveBodyNoWaitTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyNoWaitTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWaitTopic_TCP");
        assertTrue("testReceiveBodyNoWaitTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveWithTimeOutTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveWithTimeOutTopic_B_SecOn");
        assertTrue("testReceiveWithTimeOutTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveWithTimeOutTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveWithTimeOutTopic_TcpIp_SecOn");
        assertTrue("testReceiveWithTimeOutTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveNoWaitTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveNoWaitTopic_B_SecOn");
        assertTrue("testReceiveNoWaitTopic_B_SecOn failed", val);

    }

    @Test
    public void testReceiveNoWaitTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveNoWaitTopic_TcpIp_SecOn");
        assertTrue("testReceiveNoWaitTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyEmptyBodyTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyEmptyBodyTopic_B_SecOn");
        assertTrue("testReceiveBodyEmptyBodyTopic_B_SecOn failed", val);

    }

    // @Test
    public void testReceiveBodyEmptyBodyTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyEmptyBodyTopic_B_SecOn");
        assertTrue("testReceiveBodyEmptyBodyTopic_TcpIp_SecOn failed", val);

    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn");
        assertTrue(
                   "testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn");
        assertTrue(
                   "testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn");
        assertTrue("testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn failed",
                   val);

    }

    @Test
    public void testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn() throws Exception {

        boolean val = runInServlet("testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn");
        assertTrue(
                   "testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn failed",
                   val);

    }

    @ExpectedFFDC( { "com.ibm.ejs.container.UnknownLocalException","java.lang.RuntimeException" } )
    @Mode(TestMode.FULL)
    @Test
    /* 
     * MaxRedeliveryCount.
     */
    public void testRDC_BindingsAndTcpIp_SecOn() throws Exception {
        
        // Ignore the RuntimeException that is thrown by the MDB that causes message re delivery. 
        // CNTR0020E: EJB threw an unexpected (non-declared) exception during invocation of method "onMessage" on bean "BeanId"
        server.addIgnoredErrors(Arrays.asList("CNTR0020E"));
        
        server.setMarkToEndOfLog();
        // Send one message to <queue id="RedeliveryQueue1" maxRedeliveryCount="2" />
        boolean val = runInServlet("testRDC_B");        
        assertTrue("testRDC bindings servlet failed", val);
        
        String mdbOutput;
        mdbOutput = server.waitForStringInLogUsingMark("Message=1,JMSXDeliveryCount=1,JMSRedelivered=false,text=testRDC_B");
        Log.debug(c, "testRDC bindings mdbOutput="+mdbOutput);
        assertNotNull("testRDC_B failed, first redelivery not seen mdbOutput="+mdbOutput, mdbOutput);   
        mdbOutput = server.waitForStringInLogUsingMark("Message=2,JMSXDeliveryCount=2,JMSRedelivered=true,text=testRDC_B");
        Log.debug(c, "testRDC bindings mdbOutput="+mdbOutput);
        assertNotNull("testRDC_B failed, second redelivery not seen mdbOutput="+mdbOutput, mdbOutput);  
        mdbOutput = server.waitForStringInLogUsingMark("Message=3,JMSXDeliveryCount=3,JMSRedelivered=true,text=testRDC_B",1000);
        Log.debug(c, "testRDC bindings mdbOutput="+mdbOutput);
        assertNull("testRDC_B failed, third redelivery unexpectedly seen mdbOutput="+mdbOutput, mdbOutput);
//TODO Validate that the message is now in the exception destination. 

        server.setMarkToEndOfLog();
        // Send one message to remote <queue id="RedeliveryQueue1" maxRedeliveryCount="2" />
        val = runInServlet("testRDC_TcpIp");
        assertTrue("testRDC bindings servlet failed", val);
        
        mdbOutput = server.waitForStringInLogUsingMark("Message=1,JMSXDeliveryCount=1,JMSRedelivered=false,text=testRDC_TcpIp");
        Log.debug(c, "testRDC TcpIp mdbOutput="+mdbOutput);
        assertNotNull("testRDC_TcpIp failed, first redelivery not seen mdbOutput="+mdbOutput, mdbOutput);   
        mdbOutput = server.waitForStringInLogUsingMark("Message=2,JMSXDeliveryCount=2,JMSRedelivered=true,text=testRDC_TcpIp");
        Log.debug(c, "testRDC TcpIp mdbOutput="+mdbOutput);
        assertNotNull("testRDC_TcpIp failed, second redelivery not seen mdbOutput="+mdbOutput, mdbOutput);  
        mdbOutput = server.waitForStringInLogUsingMark("Message=3,JMSXDeliveryCount=3,JMSRedelivered=true,text=testRDC_TcpIp",1000);
        Log.debug(c, "testRDC TcpIp mdbOutput="+mdbOutput);
        assertNull("testRDC_TcpIp failed, third redelivery unexpectedly seen mdbOutput="+mdbOutput, mdbOutput);  
//TODO Validate that the message is now in the exception destination. 
    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" } )
    @Test
    public void testCreateSharedDurable_B_SecOn() throws Exception {

        boolean val = runInServlet("testCreateSharedDurableConsumer_create");
        
        stopAppServers();
        startAppServers();
                
        val = runInServlet("testCreateSharedDurableConsumer_consume");
        assertTrue("testCreateSharedDurable_B_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" } )
    @Test
    public void testCreateSharedDurable_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testCreateSharedDurableConsumer_create_TCP");

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateSharedDurableConsumer_consume_TCP");
        assertTrue("testCreateSharedDurable_TCP_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException"} )
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableWithMsgSel_B_SecOn() throws Exception {

        boolean val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_create");

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_consume");
        assertTrue("testCreateSharedDurableWithMsgSel_B_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException"} )
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedDurableWithMsgSel_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_create_TCP");

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateSharedDurableConsumerWithMsgSel_consume_TCP");
        assertTrue("testCreateSharedDurableWithMsgSel_TCP_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" } )
    @Test
    public void testCreateSharedNonDurable_B_SecOn() throws Exception {

        // Create a non durable subscriber, publish a message, close the context.
        boolean val = runInServlet("testCreateSharedNonDurableConsumer_create");

        // The message we just sent is deleted whether we shut down the servers or not. 
        stopAppServers();
        startAppServers();

        // Success means that the message published above is not received.
        val = runInServlet("testCreateSharedNonDurableConsumer_consume");
        assertTrue("testCreateSharedNonDurable_B_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" } )
    @Test
    public void testCreateSharedNonDurable_TCP_SecOn() throws Exception {

        // Create a non durable subscriber, publish a message, close the context.
        boolean val = runInServlet("testCreateSharedNonDurableConsumer_create_TCP");

        // The message we just sent is deleted whether we shut down the servers or not. 
        stopAppServers();
        startAppServers();

        // Success means that the message published above is not received.
        val = runInServlet("testCreateSharedNonDurableConsumer_consume_TCP");
        assertTrue("testCreateSharedNonDurable_TCP_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" } )
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableWithMsgSel_B_SecOn() throws Exception {

        boolean val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_create");

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_consume");
        assertTrue("testCreateSharedNonDurableWithMsgSel_B_SecOn failed", val);

    }

    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" } )
    @Mode(TestMode.FULL)
    @Test
    public void testCreateSharedNonDurableWithMsgSel_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_create_TCP");

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateSharedNonDurableConsumerWithMsgSel_consume_TCP");
        assertTrue("testCreateSharedNonDurableWithMsgSel_TCP_SecOn failed", val);

    }

    @Test
    public void testMultiSharedNonDurableConsumer_SecOn() throws Exception {
        server.setMarkToEndOfLog();
        boolean val = runInServlet("testBasicMDBTopic");
        assertTrue(methodName()+" Failed to send messages", val);
        
        // The servlet has sent 3 distinct messages which are received by either MDB1 or MDB2,
        // the MDB's run as multiple instances so the order the messages are received is unpredictable.
        // We allow up to 120 seconds to receive all of the messages,
        // although normally there should be minimal delay and anything more that 10 seconds means that the test infrastructure is not 
        // providing enough resources.
        long receiveStartMilliseconds = System.currentTimeMillis();
        int count = server.waitForMultipleStringsInLogUsingMark(3, "Received in MDB[1-2]: testBasicMDBTopic:");
        Log.debug(c, "Bindings count="+count);
        assertEquals("Incorrect number of messages:"+count, count, 3);
        long receiveMilliseconds = System.currentTimeMillis()-receiveStartMilliseconds;
        assertTrue("Test infrastructure failure, excessive time to receive:"+receiveMilliseconds, receiveMilliseconds<10*1000);
       
        server.setMarkToEndOfLog();
        val = runInServlet("testBasicMDBTopic_TCP");
        assertTrue("testMultiSharedNonDurableConsumer_SecOn Failed to send messages", val);
        
        receiveStartMilliseconds = System.currentTimeMillis();
        count = server.waitForMultipleStringsInLogUsingMark(3, "Received in MDB[1-2]: testBasicMDBTopic_TCP:");
        Log.debug(c, "TCP count="+count);
        assertEquals("Incorrect number of messages:"+count, count, 3);
        receiveMilliseconds = System.currentTimeMillis()-receiveStartMilliseconds;
        assertTrue("Test infrastructure failure, excessive time to receive:"+receiveMilliseconds, receiveMilliseconds<10*1000);
    }

    @Test
    public void testMultiSharedDurableConsumer_SecOn() throws Exception {
        server.setMarkToEndOfLog();
        boolean val = runInServlet("testBasicMDBTopicDurShared");
        assertTrue(methodName()+" Failed to send messages", val);
        
        long receiveStartMilliseconds = System.currentTimeMillis();
        int count = server.waitForMultipleStringsInLogUsingMark(3, "Received in MDB[1-2]: testBasicMDBTopic:");
        Log.debug(c, "Bindings count="+count);
        assertEquals("Incorrect number of messages:"+count, count, 3);
        long receiveMilliseconds = System.currentTimeMillis()-receiveStartMilliseconds;
        assertTrue("Test infrastructure failure, excessive time to receive:"+receiveMilliseconds, receiveMilliseconds<10*1000);
              
        server.setMarkToEndOfLog();
        val = runInServlet("testBasicMDBTopicDurShared_TCP");
        receiveStartMilliseconds = System.currentTimeMillis();
        count = server.waitForMultipleStringsInLogUsingMark(3, "Received in MDB[1-2]: testBasicMDBTopic_TCP:");
        Log.debug(c, "TCP count="+count);
        assertEquals("Incorrect number of messages:"+count, count, 3);
        receiveMilliseconds = System.currentTimeMillis()-receiveStartMilliseconds;
        assertTrue("Test infrastructure failure, excessive time to receive:"+receiveMilliseconds, receiveMilliseconds<10*1000);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSetMessageProperty_Bindings_SecOn() throws Exception {

        boolean val = runInServlet("testSetMessageProperty_Bindings_SecOn");

        assertTrue("testSetMessageProperty_Bindings_SecOn failed", val);

    }

    // 118067_9 : Test setting message properties on createProducer using method
    // chaining.
    // Message send options may be specified using one or more of the following
    // methods: setDeliveryMode, setPriority, setTimeToLive, setDeliveryDelay,
    // setDisableMessageTimestamp, setDisableMessageID and setAsync.
    // TCP/IP and Security off

    @Mode(TestMode.FULL)
    @Test
    public void testSetMessageProperty_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testSetMessageProperty_TCP_SecOn");
        assertTrue("testSetMessageProperty_TCP_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_B_SecOn() throws Exception {

        boolean val = runInServlet("testTopicName_temp_B");
        assertTrue("testTopicName_temp_B_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTopicName_temp_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testTopicName_temp_TCP");
        assertTrue("testTopicName_temp_TCP_SecOn failed", val);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_Bindings_SecOn() throws Exception {

        server.setMarkToEndOfLog();
        boolean val = runInServlet("testQueueNameCaseSensitive_Bindings");
        assertTrue("testQueueNameCaseSensitive_Bindings_SecOn failed", val);
        // We should see CWSIK0015E: The destination queue1 was not found on messaging engine defaultME.
        String waitFor = server.waitForStringInLogUsingMark("CWSIK0015E.*queue1.*");
        assertNotNull("Server CWSIK0015E message not found", waitFor);
        server.addIgnoredErrors(Arrays.asList("CWSIK0015E"));       

    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueueNameCaseSensitive_TCP_SecOn() throws Exception {

        server1.setMarkToEndOfLog();
        boolean val = runInServlet("testQueueNameCaseSensitive_TCP");
        assertTrue("testQueueNameCaseSensitive_TCP_SecOn failed", val);
        // We should see CWSIK0015E: The destination queue1 was not found on messaging engine defaultME.
        String waitFor = server1.waitForStringInLogUsingMark("CWSIK0015E.*queue1.*");
        assertNotNull("Server CWSIK0015E message not found", waitFor);
        server1.addIgnoredErrors(Arrays.asList("CWSIK0015E"));

    }

    // end 118077
    @org.junit.AfterClass
    public static void tearDown() {
        try {
            stopAppServers();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException("tearDown exception", exception);
        }
        
        ShrinkHelper.cleanAllExportedArchives();
    }
}
