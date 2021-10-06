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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

// Capping this test to JDK 8, because the CXF libs checked in only work with the JDK's copy of JAX-B (which was removed in JDK 9)
@MaximumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, JakartaEE9Action.ID })
@Mode(TestMode.FULL)
public class PureCXFTest {

    @Server("PureCXFTestServer")
    public static LibertyServer server;

    private static final int CONN_TIMEOUT = 5;
    private static String clientUrlStr;

    private static final String pureCXF = "publish/shared/resources/pureCXF/";

    @BeforeClass
    public static void setUp() throws Exception {
        File[] lib = new File(pureCXF).listFiles();
        WebArchive pureCXF = ShrinkHelper.buildDefaultApp("pureCXF", "com.ibm.samples.jaxws.spring.service",
                                                          "com.ibm.ws.jaxws.fat.util");
        pureCXF.addAsLibraries(lib);
        ShrinkHelper.exportDropinAppToServer(server, pureCXF);
        server.addInstalledAppForValidation("pureCXF");

        WebArchive pureCXFWSDLFirst = ShrinkHelper.buildDefaultApp("pureCXFWSDLFirst", "com.ibm.samples.jaxws.spring.wsdlfirst.client",
                                                                   "com.ibm.samples.jaxws.spring.wsdlfirst.service",
                                                                   "com.ibm.samples.jaxws.spring.wsdlfirst.stub",
                                                                   "org.w3._2001.xmlschema",
                                                                   "com.ibm.ws.jaxws.fat.util");
        pureCXFWSDLFirst.addAsLibraries(lib);
        ShrinkHelper.exportDropinAppToServer(server, pureCXFWSDLFirst);
        server.addInstalledAppForValidation("pureCXFWSDLFirst");

        server.startServer();
        assertNotNull("Application pureCXF does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*pureCXF"));
        assertNotNull("Application pureCXFWSDLFirst does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*pureCXFWSDLFirst"));
        clientUrlStr = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).toString();
    }

    @Test
    public void testPureCXF() throws Exception {
        String servletName = "HelloWorldClientServlet";

        URL url = new URL(clientUrlStr + "/pureCXF/" + servletName);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull("The output is null", line);
        assertTrue("Response is not correct.", line.equals("Hello Joe"));

    }

    @Test
    public void testPureCXFWSDLFirstGetCustomer() throws Exception {
        String servletName = "CustomerServiceClientGetCustomerServlet";

        URL url = new URL(clientUrlStr + "/pureCXFWSDLFirst/" + servletName);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull("The output is null", line);
        assertTrue("Response is not correct.", line.equals("Smith"));

    }

    @Test
    public void testPureCXFWSDLFirstGetCustomerByImpl() throws Exception {
        String servletName = "CustomerServiceClientGetCustomerByImplServlet";

        URL url = new URL(clientUrlStr + "/pureCXFWSDLFirst/" + servletName);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull("The output is null", line);
        assertTrue("Response is not correct.", line.equals("Joe"));

    }

    @Test
    public void testPureCXFWSDLFirstNoSuchCustomer() throws Exception {
        String servletName = "CustomerServiceClientNoSuchCustomerServlet";

        URL url = new URL(clientUrlStr + "/pureCXFWSDLFirst/" + servletName);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull("The output is null", line);
        assertTrue("Response is not correct.", line.contains("Did not find any matching customer"));

    }

    @Test
    public void testPureCXFWSDLFirstUpdateCustomer() throws Exception {
        String servletName = "CustomerServiceClientUpdateCustomerServlet";

        URL url = new URL(clientUrlStr + "/pureCXFWSDLFirst/" + servletName);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull("The output is null", line);
        assertTrue("Response is not correct.", line.contains("Customer update done."));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE9967W");
        }
    }

}
