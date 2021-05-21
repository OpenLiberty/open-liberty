/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class JAXRSClientSSLTestNoLibertySSLCfg extends AbstractTest {

    @Server("jaxrs20.client.JAXRSClientSSLTestNoLibertySSLCfg")
    public static LibertyServer serverNoSSL;

    @Server("jaxrs20.client.JAXRSClientSSLTest")
    public static LibertyServer server;

    protected final static String appname = "jaxrsclientssl";
    protected final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.JAXRSClientSSL.client",
                                                       "com.ibm.ws.jaxrs20.client.JAXRSClientSSL.service");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            serverNoSSL.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        
        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on serverNoSSL",
                      serverNoSSL.waitForStringInLog("CWWKF0011I"));
        
        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        serverNoSSL.addIgnoredErrors(Arrays.asList("CWWKO0801E"));
        serverNoSSL.stopServer();
        server.addIgnoredErrors(Arrays.asList("CWWKO0801E"));
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
    public void testClientNoLibertySSL_ClientBuilder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        runTestOnServer(target, "testClientBasicSSLDefault", p, "[Basic Resource]:alex");
    }

    @Test
    public void testClientLtpaHander_ClientNoTokenWithNoLibertySSL() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "jordan");
        runTestOnServer(target, "testClientLtpaHander_ClientNoTokenWithSSLDefault", p, "[Basic Resource]:jordan");
    }

    @Override
    protected void runTestOnServer(String target, String testMethod, Map<String, String> params,
                                   String... expectedResponses) throws ProtocolException, MalformedURLException, IOException {

        //build basic URI
        StringBuilder sBuilder = new StringBuilder("http://").append(serverNoSSL.getHostname()).append(":").append(serverNoSSL.getHttpDefaultPort()).append("/").append(target).append("?test=").append(testMethod);

        //add params to URI
        if (params != null && params.size() > 0) {

            StringBuilder paramStr = new StringBuilder();

            Iterator<String> itr = params.keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                paramStr.append("&@" + key + "=" + params.get(key));
            }

            sBuilder.append(paramStr.toString());
        }

        sBuilder.append("&@secport=" + server.getHttpDefaultSecurePort());
        sBuilder.append("&@hostname=" + server.getHostname());

        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testMethod, "Calling ClientTestApp with URL=" + urlStr);

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        Log.info(this.getClass(), testMethod, "The response: " + line);
        boolean foundExpectedResponse = false;
        for (String expectedResponse : expectedResponses) {
            if (line.contains(expectedResponse)) {
                foundExpectedResponse = true;
                break;
            }
        }
        assertTrue("Real response is " + line + " and the expected response is one of " + String.join(" | ", expectedResponses),  foundExpectedResponse);
    }
}
