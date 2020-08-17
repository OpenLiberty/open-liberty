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
package com.ibm.ws.jaxrs20.fat.callback;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class JAXRS20CallBackTest {

    @Server("com.ibm.ws.jaxrs.fat.callback")
    public static LibertyServer server;

    public static final String RESUMED = "Response resumed";
    public static final String ISE = "Illegal State Exception Thrown";
    public static final String NOE = "No Exception Thrown";
    public static final String FALSE = "A method returned false";
    public static final String TRUE = "A method return true";

    private final static int REQUEST_TIMEOUT = 10;
    private static final String CONTEXT_ROOT = "callback";
    private static String BASE_URL;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, CONTEXT_ROOT, "com.ibm.ws.jaxrs.fat.callback");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            BASE_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + CONTEXT_ROOT + "/";
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    @AllowedFFDC("java.lang.RuntimeException")
    public void testargumentContainsExceptionWhenSendingIoException() throws ProtocolException, MalformedURLException, IOException
    {
        //build basic URI
        StringBuilder sBuilder = new StringBuilder(BASE_URL).append("callbackServlet?test=testargumentContainsExceptionWhenSendingIoException");

        String urlStr = sBuilder.toString();

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertEquals("success", line);
    }

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC("java.lang.RuntimeException")
    public void testargumentContainsExceptionInTwoCallbackObjects() throws Exception {
        //build basic URI
        StringBuilder sBuilder = new StringBuilder(BASE_URL).append("callbackServlet?test=testargumentContainsExceptionInTwoCallbackObjects");

        String urlStr = sBuilder.toString();

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertEquals("successsuccesssuccesssuccess",line);
    }

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC("java.lang.RuntimeException")
    public void testargumentContainsExceptionInTwoCallbackClasses() throws Exception {
        //build basic URI
        StringBuilder sBuilder = new StringBuilder(BASE_URL).append("callbackServlet?test=testargumentContainsExceptionInTwoCallbackClasses");

        String urlStr = sBuilder.toString();

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        System.out.println(line);
        assertEquals("successsuccesssuccesssuccess", line);
    }
}
