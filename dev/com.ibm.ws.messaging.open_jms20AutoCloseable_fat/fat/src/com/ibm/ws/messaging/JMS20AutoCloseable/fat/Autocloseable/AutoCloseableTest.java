/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20AutoCloseable.fat.Autocloseable;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.messaging.JMS20AutoCloseable.fat.TestUtils;

@RunWith(FATRunner.class)
public class AutoCloseableTest {
    private static LibertyServer server =
        LibertyServerFactory.getLibertyServer("TestServer");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean runInServlet(String test) throws IOException {
        URL url = new URL("http://" + HOST + ":" + PORT + "/AutoCloseable?test=" + test);
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
            for ( String line = br.readLine(); line != null; line = br.readLine() ) {
                lines.append(line).append(sep);
            }

            boolean result;
            if ( lines.indexOf("COMPLETED SUCCESSFULLY") < 0 ) {
                fail("Missing success message in output. " + lines);
                result = false;
            } else {
                result = true;
            }
            return result;

        } finally {
            con.disconnect();
        }
    }

    //

    @BeforeClass
    public static void testConfigFileChange() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
        server.setServerConfigurationFile("Qclose.xml");
        TestUtils.addDropinsWebApp(server, "AutoCloseable", "web");

        server.startServer("AutoCloseable.log");
    }

    @AfterClass
    public static void tearDown() {
        try {
            server.stopServer();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //

    // This test case will test the AutoCloseable feature for QueueConnection
    @Test
    public void testQConnClose() throws Exception {
        boolean testResult = runInServlet("testQueueConnectionClose");
        assertTrue("testQConnClose Failed", testResult);
    }

    // This test case will test the AutoCloseable feature for QueueSession
    @Test
    public void testQSessionClose() throws Exception {
        boolean testResult = runInServlet("testQueueSessionClose");
        assertTrue("testQSessionClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for QueueSender
    @Test
    public void testQSenderClose() throws Exception {
        boolean testResult = runInServlet("testQueueSenderClose");
        assertTrue("testQSenderClose Failed", testResult);
    }

    // This test case will test the AutoCloseable feature for QueueBrowser
    @Test
    public void testQBrowserClose() throws Exception {
        boolean testResult = runInServlet("testQueueBrowserClose");
        assertTrue("testQBrowserClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for TopicConnection
    @Test
    public void testTConnectionClose() throws Exception {
        boolean testResult = runInServlet("testTopicConnectionClose");
        assertTrue("testTConnectionClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for TopicSession
    @Test
    public void testTSessionClose() throws Exception {
        boolean testResult = runInServlet("testTopicSessionClose");
        assertTrue("testTSessionClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for TopicSubscriber
    @Test
    public void testTSubscriberClose() throws Exception {
        boolean testResult = runInServlet("testTopicSubscriberClose");
        assertTrue("testTSubscriberClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for TopicPublisher
    @Test
    public void testTPublisherClose() throws Exception {
        boolean testResult = runInServlet("testTopicPublisherClose");
        assertTrue("testTopicPublisherClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for QueueReceiver
    @Test
    public void testQReceiverClose() throws Exception {
        boolean testResult = runInServlet("testQueueReceiverClose");
        assertTrue("testQueueReceiverClose failed", testResult);

    }

    // This test case will test the AutoCloseable feature for JMSContext
    @ExpectedFFDC(value = "javax.jms.JMSException")
    @Test
    public void testJMSContextClose() throws Exception {
        boolean testResult = runInServlet("testJMSContextClose");
        assertTrue("testJMSContextClose failed", testResult);
    }

    // This test case will test the AutoCloseable feature for JMSConsumer
    @Test
    public void testJMSConsumerClose() throws Exception {
        boolean testResult = runInServlet("testJMSConsumerClose");
        assertTrue("testJMSConsumerClose failed", testResult);
    }
}
