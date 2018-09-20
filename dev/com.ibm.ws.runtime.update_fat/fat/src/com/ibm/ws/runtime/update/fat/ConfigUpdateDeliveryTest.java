/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests that applications on the Liberty profile can access the RuntimeUpdateNotificationMBean, register
 * a NotificationListener and receive notifications for updates to the server.xml configuration.
 */
public class ConfigUpdateDeliveryTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.runtime.update.fat");
    private static final String urlPrefix = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/mbean";
    private final Class<?> c = ConfigUpdateDeliveryTest.class;

    @Rule
    public final TestName method = new TestName();

    @Test
    public void configUpdatesDelivered() throws Exception {
        Log.info(c, method.getMethodName(), "Entering test: " + method.getMethodName());

        // Add a notification listener to the MBeanServer.
        URL url = new URL(urlPrefix + "?setupNotificationListener");
        Log.info(c, "testConfigUpdatesDelivered", "Calling JMX Servlet with URL=" + url.toString());
        testServlet(url);

        // Replace the server.xml configuration file to trigger a notification for the delivery of config updates.
        Log.info(c, method.getMethodName(), "Replacing: " + server.getServerName() +
                                            " server.xml to trigger a runtime update notification.");
        server.setServerConfigurationFile("local-connector-server.xml");
        // Wait for the configuration update to take effect
        assertNotNull("FAIL: " + server.getServerName() + " did not complete the server configuration update",
                      server.waitForStringInLog("CWWKG0017I:.*"));

        // Wait for the notification to be received for the configuration update.
        url = new URL(urlPrefix + "?checkForNotifications");
        Log.info(c, "testConfigUpdatesDelivered", "Calling JMX Servlet with URL=" + url.toString());
        testServlet(url);

        Log.info(c, method.getMethodName(), "Exiting test: " + method.getMethodName());
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();

        WebArchive dropinsApp = ShrinkHelper.buildDefaultApp("mbean", "web");
        ShrinkHelper.exportDropinAppToServer(server, dropinsApp);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private void testServlet(URL url) throws IOException {
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        try {
            // read the page contents
            String line = br.readLine();
            List<String> lines = new ArrayList<String>();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
            con.disconnect();

            // log the output lines so we can debug
            System.out.println("Output: ");
            for (String msg : lines) {
                System.out.println(msg);
            }

            // check the first line to be sure we at least got to the servlet
            assertEquals("This is a servlet for the RuntimeUpdateNotificationMBean.", lines.get(0));

            boolean foundPass = false;

            // Pass criteria:
            // - No FAIL: lines
            // - at least one PASS line

            for (String msg : lines) {
                if (msg.startsWith("FAIL: ")) {
                    // When there is a fail log the whole output
                    StringBuilder builder = new StringBuilder();
                    for (String lineForMessage : lines) {
                        builder.append(lineForMessage);
                        builder.append("\n");
                    }
                    fail(builder.toString());
                }
                if (msg.startsWith("PASS")) {
                    foundPass = true;
                }
            }
            if (!foundPass) {
                fail("Did not see PASS from servlet invocation at " + url);
            }

        } finally {
            br.close();
        }
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }
}