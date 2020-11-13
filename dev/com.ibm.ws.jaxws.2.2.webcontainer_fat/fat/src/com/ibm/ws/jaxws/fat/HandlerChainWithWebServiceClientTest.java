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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)

public class HandlerChainWithWebServiceClientTest {

    @Server("HandlerChainWithWebServiceClientTest")
    public static LibertyServer server;

    private static final String SERVLET_PATH = "/addNumbersClient/AddNumbersTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "addNumbersProvider", "com.ibm.ws.jaxws.provider");

        ShrinkHelper.defaultDropinApp(server, "addNumbersClient", "com.ibm.ws.jaxws.client",
                                      "com.ibm.ws.jaxws.client.servlet");

        server.copyFileToLibertyServerRoot("", "HandlerChainWithWebServiceClientTest/AddNumbers.wsdl");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testHandlerChainWithWebServiceClient() throws Exception {

        String actual = invokeService(1, 2);
        String expected = "Result = 3";
        assertTrue("Expected output to contain \"" + expected
                   + "\", but instead it contained: " + actual + ".",
                   actual.indexOf(expected) != -1);
        assertStatesExistedFromMark(true, 5000, new String[] {
                                                               "com.ibm.ws.jaxws.client.MyHandler: handle outbound message",
                                                               "com.ibm.ws.jaxws.client.MyHandler: handle inbound message",
        });

    }

    private String invokeService(int num1, int num2) throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?number1=").append(Integer.toString(num1)).append("&number2=").append(Integer.toString(num2));
        String urlStr = sBuilder.toString();

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 5);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line;

    }

    private void assertStatesExistedFromMark(boolean needReset, long timeout, String... states) {
        if (needReset) {
            server.resetLogOffsets();
        }

        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLogUsingMark(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }
}
