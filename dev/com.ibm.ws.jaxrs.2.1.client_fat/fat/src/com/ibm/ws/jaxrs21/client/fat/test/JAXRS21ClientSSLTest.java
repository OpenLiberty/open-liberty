/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.fat.test;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS21ClientSSLTest extends JAXRS21AbstractTest {
    @Server("jaxrs21.client.JAXRS21ClientSSLTest")
    public static LibertyServer server;

    private static final String clientsslwar = "jaxrs21clientssl";

    private final static String target = "jaxrs21clientssl/JAXRS21ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, clientsslwar,
                                      "com.ibm.ws.jaxrs21.client.JAXRS21ClientSSL.client",
                                      "com.ibm.ws.jaxrs21.client.JAXRS21ClientSSL.service");


        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));

        // wait for the tcp channel to start
        assertNotNull("TCP Channel not started",
                      server.waitForStringInLog("CWWKO0219I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWWKO0801E"));
        server.stopServer();
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
    public void testClientBasicSSL_ClientBuilder() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testClientBasicSSL_ClientBuilder", p, "[Basic Resource]:alex");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Currently, RESTEasy only allows SSLContext to be set from ClientBuilder
    public void testClientBasicSSL_Client() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testClientBasicSSL_Client", p, "[Basic Resource]:alex");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Currently, RESTEasy only allows SSLContext to be set from ClientBuilder
    public void testClientBasicSSL_WebTarget() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testClientBasicSSL_WebTarget", p, "[Basic Resource]:alex");
    }

    // @Test
    public void testClientBasicSSL_InvalidSSLRef() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testClientBasicSSL_InvalidSSLRef", p, "the SSL configuration reference \"invalidSSLConfig\" is invalid.");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Needs more investigation, but also runs into the same issue with SSLContext only being set from ClientBuilder
    public void testClientBasicSSL_CustomizedSSLContext() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testClientBasicSSL_CustomizedSSLContext", p, "unable to find valid certification path to requested target");
    }
}
