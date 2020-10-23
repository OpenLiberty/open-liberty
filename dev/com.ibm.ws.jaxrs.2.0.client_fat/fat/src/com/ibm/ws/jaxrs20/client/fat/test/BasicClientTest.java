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
package com.ibm.ws.jaxrs20.client.fat.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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
public class BasicClientTest extends AbstractTest {

    @Server("jaxrs20.client.BasicClientTest")
    public static LibertyServer server;

    private final static String appname = "baseclient";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.BasicClientTest.client",
                                                       "com.ibm.ws.jaxrs20.client.BasicClientTest.service");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
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

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testNewClientBuilder() throws Exception {

        this.runTestOnServer(target, "testNewClientBuilder", null, "OK");
    }

    @Test
    public void testNewClient() throws Exception {

        this.runTestOnServer(target, "testNewClient", null, "OK");
    }

    @Test
    public void testNewWebTarget() throws Exception {

        this.runTestOnServer(target, "testNewWebTarget", null, "OK");
    }

    @Test
    public void testNewWebTargetSupportMapType() throws Exception {

        this.runTestOnServer(target, "testNewWebTargetSupportMapType", null, "OK");
    }

    @Test
    public void testNewInvocationBuilder() throws Exception {

        this.runTestOnServer(target, "testNewInvocationBuilder", null, "OK");

    }

    @Test
    public void testNewInvocation() throws Exception {

        this.runTestOnServer(target, "testNewInvocation", null, "OK");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // currently broken 
    public void testDefaultAccept() throws Exception {

        this.runTestOnServer(target, "testDefaultAccept", null, "Hello World");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // currently broken 
    public void testFlowProgram() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testFlowProgram", p, "[Basic Resource]:alex");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // currently broken 
    public void testQueryParam() throws Exception {
        this.runTestOnServer(target, "testQueryParam", null, "OK");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // currently broken 
    public void testQueryParamWebcontainerNoEquals() throws Exception {
        String uri = "http://" + serverRef.getHostname()
                     + ":"
                     + serverRef.getHttpDefaultPort()
                     + "/baseclient/BasicClientTest/BasicResource/query?param";

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(uri), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        StringBuilder logOutput = new StringBuilder();
        for (String nextLine = line; nextLine != null; nextLine = br.readLine()) {
            logOutput.append(nextLine);
        }

        Log.info(this.getClass(), "testQueryParam2", "The response: " + logOutput.toString());

        // we expect the query parameter to be null on the server, even when webcontainer processes it,
        // because in CXT setting a query parameter to null removes all all values.
        assertTrue("Real response is \"" + line + "\" and the expected response is \"null\"", line.contains("null"));
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // currently broken 
    public void testQueryParamWebcontainerEquals() throws Exception {
        String uri = "http://" + serverRef.getHostname()
                      + ":"
                      + serverRef.getHttpDefaultPort()
                     + "/baseclient/BasicClientTest/BasicResource/query?param=";

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(uri), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        StringBuilder logOutput = new StringBuilder();
        for (String nextLine = line; nextLine != null; nextLine = br.readLine()) {
            logOutput.append(nextLine);
        }

        Log.info(this.getClass(), "testQueryParam2", "The response: " + logOutput.toString());
        assertTrue("Real response is \"" + line + "\" and the expected response is \"param\"", line.contains("param"));
    }
}