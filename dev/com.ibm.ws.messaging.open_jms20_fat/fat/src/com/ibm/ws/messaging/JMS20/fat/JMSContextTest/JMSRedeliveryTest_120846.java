/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSContextTest;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class JMSRedeliveryTest_120846 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private boolean val = false;

    private boolean runInServlet(String test) throws IOException {

        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSRedelivery_120846?test="
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
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            }
            else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");

        server1.setServerConfigurationFile("JMSContext_Server.xml");
        server1.startServer("JMSRedelivery_120846_Server.log");

        server.setServerConfigurationFile("JMSContext_Client.xml");
        server.startServer("JMSRedelivery_120846_Client.log");

    }

    @org.junit.AfterClass
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

    //When a client receives a message the mandatory JMS-defined message property JMSXDeliveryCount will be set to the number of times the message has been delivered. The first time a message is received it will be set to 1
    @Test
    public void testInitialJMSXDeliveryCount_B_SecOff() throws Exception {

        val = runInServlet("testInitialJMSXDeliveryCount_B_SecOff");
        assertTrue("testInitialJMSXDeliveryCount_B_SecOff failed", val);
    }

    @Test
    public void testInitialJMSXDeliveryCount_TcpIp_SecOff() throws Exception {

        val = runInServlet("testInitialJMSXDeliveryCount_TcpIp_SecOff");
        assertTrue("testInitialJMSXDeliveryCount_TcpIp_SecOff failed", val);

    }

    //Test with message redelivery : value of 2 or more means the message has been redelivered
    //If the JMSRedelivered message header value is set then the JMSXDeliveryCount property must always be 2 or more.
    //Test with duplicate delivery of messages
    @Mode(TestMode.FULL)
//    @ExpectedFFDC(value = { "java.lang.reflect.InvocationTargetException",
//                           "java.lang.RuntimeException",
//                           "com.ibm.ejs.container.UnknownLocalException" })
    @Test
    public void testRDC_BindingsAndTcpIp_SecOff() throws Exception {

        server1.stopServer();
        server.stopServer();

        server.setServerConfigurationFile("120846_Bindings.xml");

        server1.startServer();
        server.startServer();

        val = runInServlet("testRDC_B");

        assertTrue("testRDC_B failed", val);

        val = runInServlet("testMaxRDC_B");

        assertTrue("testMaxRDC_B failed", val);

        server1.stopServer();
        server.stopServer();

        server.setServerConfigurationFile("120846_TcpIp.xml");

        server1.startServer();
        server.startServer();

        val = runInServlet("testRDC_TcpIp");

        assertTrue("testRDC_TcpIp failed", val);

        val = runInServlet("testMaxRDC_TcpIp");

        assertTrue("testMaxRDC_TcpIp failed", val);

        server.stopServer();
        server1.stopServer();

        server.setServerConfigurationFile("JMSContext_Client.xml");

        server1.startServer();
        server.startServer();

    }

    //Test to validate targetTransportChains
    @Mode(TestMode.FULL)
    @Test
    public void testTargetTransportChainTcpIp_SecOff() throws Exception {

        server1.stopServer();
        server.stopServer();

        server.setServerConfigurationFile("185047_Bindings.xml");

        server1.startServer();
        server.startServer();

        val = runInServlet("testTargetChain_B");

        assertTrue("testTargetChain_B failed", val);

        server.stopServer();
        server1.stopServer();

        server.setServerConfigurationFile("JMSContext_Client.xml");

        server1.startServer();
        server.startServer();

    }

}
