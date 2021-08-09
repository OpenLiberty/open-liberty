/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class BasicTest {

    @Server("com.ibm.ws.jaxws.cdi.fat.simpleTestService")
    public static LibertyServer server;

    private final static String ignoreErrorOrWarningMsg = ".SRVE0274W.";
    private final static int REQUEST_TIMEOUT = 60;
    private final static String appName = "simpleTestService";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackages(false, "com.ibm.ws.jaxws.cdi.beans")
                        .addPackages(false, "com.ibm.ws.jaxws.cdi.handler")
                        .addPackages(true, "com.ibm.ws.jaxws.cdi.service")
                        .addPackages(false, "com.ibm.ws.jaxws.cdi.testservice.client");

        ShrinkHelper.addDirectory(war, "test-applications/" + appName + "/resources");

        ShrinkHelper.exportDropinAppToServer(server, war);

        String host = server.getHostname();
        String port = Integer.toString(server.getPort(PortType.WC_defaulthost));

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> variables = config.getVariables();
        variables.add(new Variable("host", host));
        variables.add(new Variable("port", port));

        server.updateServerConfiguration(config);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(ignoreErrorOrWarningMsg); // trust stop server to ensure server has
            // stopped
        }
    }

    @Test
    public void testSimpleImplService() throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/simpleTestService/SimpleServiceServlet")
                        .append("?port=")
                        .append(server.getHttpDefaultPort())
                        .append("&hostname=")
                        .append(server.getHostname())
                        .append("&service=")
                        .append("SimpleImplService")
                        .append("&war=simpleTestService")
                        .append("&arg0=Hello");
        server.waitForStringInLog("CWWKZ0001I.*simpleTestService");
        String urlStr = sBuilder.toString();
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr),
                                                            HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        System.out.println("result: " + line);
        br.close();
        Assert.assertEquals("Hello,I'm student.", line);

        assertLibertyMessage("Student's post constructor called", 3, "less");
        assertLibertyMessage("I'm teacher. in LogicalHandlerImpl", 1, "equal");
        assertLibertyMessage("I'm teacher. in handleMessage", 1, "equal");

    }

//    @Test
    public void testSimpleJAXWSWebserviceProvider() throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/simpleTestService/SimpleServiceServlet")
                        .append("?port=")
                        .append(server.getHttpDefaultPort())
                        .append("&hostname=")
                        .append(server.getHostname())
                        .append("&service=")
                        .append("SimpleImplProviderService")
                        .append("&war=simpleTestService")
                        .append("&arg0=Hello");
        server.waitForStringInLog("CWWKZ0001I.*simpleTestService");
        String urlStr = sBuilder.toString();
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr),
                                                            HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        System.out.println("result: " + line);
        br.close();
        Assert.assertEquals("Hello,I'm student.", line);
        assertLibertyMessage("Student's post constructor called", 3, "less");
    }

    private void assertLibertyMessage(String message, int number, String equal) {
        try {
            List<String> messages = server.findStringsInLogs(message, server.getConsoleLogFile());
            if (equal.equals("more")) {
                Assert.assertTrue("Expect to get CDI test message more than " + number + ": " + messages, messages.size() > number);

            } else if (equal.equals("less")) {
                //Why use this equal param? Because for each PerRequest request, the message will be report, so there will be several messages in logs
                //Also because in other jdk such as oracle jdk, maybe the execution order is unexpectable
                Assert.assertTrue("Expect to get CDI test message less than " + number + ": " + messages, messages.size() <= number);
            } else {
                Assert.assertTrue("Expect to get CDI test messages equal " + number + ": " + messages, messages.size() == number);
            }
        } catch (Exception e) {
            Assert.assertTrue("Get Exception " + e.getMessage() + " when assertLibertyMessage", false);
        }
    }

}
