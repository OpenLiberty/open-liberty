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
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@SkipForRepeat("jaxws-2.3")
public class WebServiceRefFeaturesTest {

    @Server("WebServiceRefFeaturesTestServer")
    public static LibertyServer server;

    private final Class<?> c = WebServiceRefFeaturesTest.class;
    private static final int CONN_TIMEOUT = 10;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "webServiceRefFeatures", "com.ibm.ws.test.client.stub",
                                      "com.ibm.ws.test.wsfeatures.client",
                                      "com.ibm.ws.test.wsfeatures.client.handler",
                                      "com.ibm.ws.test.wsfeatures.handler",
                                      "com.ibm.ws.test.wsfeatures.service");

        server.startServer("WebServiceRefFeaturesTest.log");
        server.waitForStringInLog("webServiceRefFeatures");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testWSFeaturesForPortInjection() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/webServiceRefFeatures/port");
        Log.info(c, "testWSFeaturesForPortInjection",
                 "Calling Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        br.readLine();

        //Check @MTOM
        String multipartStr = server.waitForStringInLog("multipart/related");
        assertNotNull("@MTOM feature is not supported", multipartStr);

        //Check @Addressing
        String addressToStr = server.waitForStringInLog("addressing}To");
        assertNotNull("@Addressing feature is not supported", addressToStr);
    }

    @Test
    public void testWSFeaturesForServiceInjection() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/webServiceRefFeatures/service");
        Log.info(c, "testWSFeaturesForServiceInjection",
                 "Calling Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        br.readLine();

        //Check @MTOM
        String multipartStr = server.waitForStringInLog("multipart/related");
        assertNotNull("@MTOM feature is not supported", multipartStr);

        //Check @Addressing
        String addressReplyToStr = server.waitForStringInLog("addressing}ReplyTo");
        assertNotNull("@Addressing feature is not supported", addressReplyToStr);
    }

    @Test
    public void testWSAddressingEnabledInWSDL() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/webServiceRefFeatures/wsaclient");
        Log.info(c, "testWSAddressingEnabledInWSDL",
                 "Calling Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        br.readLine();

        final String prefix = "ImageServiceImplServiceTwo.uploadImage response header:";

        String exceptionInLog = server.waitForStringInLog("javax.xml.soap.SOAPException: Did not receive any or all expected WS-Addressing headers in response");
        assertNull("Handler threw SOAPException because required WS-Addressing headers not present", exceptionInLog);
    }
}
