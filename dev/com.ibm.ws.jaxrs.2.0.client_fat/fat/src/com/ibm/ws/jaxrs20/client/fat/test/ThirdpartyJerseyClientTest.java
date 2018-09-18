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

import java.io.File;
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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ThirdpartyJerseyClientTest extends AbstractTest {

    @Server("jaxrs20.client.ThirdpartyJerseyClientTest")
    public static LibertyServer server;

    private final static String appname = "thirdpartyjerseyclient";
    private final static String targetpf = appname + "pf/ClientTestServlet";
    private final static String targetpl = appname + "pl/ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {
        File libs = new File("publish/shared/resources/thirdpartyjerseyclient/");
        WebArchive thirdpartyjerseyclientpf = ShrinkHelper.buildDefaultApp(appname + "pf",
                                                       "com.ibm.ws.jaxrs20.client.ThirdpartyJerseyClient",
                                                       "com.ibm.ws.jaxrs20.client.ThirdpartyJerseyClient.service");
        thirdpartyjerseyclientpf.addAsLibraries(libs.listFiles());
        ShrinkHelper.exportAppToServer(server, thirdpartyjerseyclientpf);
        server.addInstalledAppForValidation(appname + "pf");

        WebArchive thirdpartyjerseyclientpl = ShrinkHelper.buildDefaultApp(appname + "pl",
                                                      "com.ibm.ws.jaxrs20.client.ThirdpartyJerseyClient",
                                                      "com.ibm.ws.jaxrs20.client.ThirdpartyJerseyClient.service");
        thirdpartyjerseyclientpl.addAsLibraries(libs.listFiles());
        ShrinkHelper.exportAppToServer(server, thirdpartyjerseyclientpl);
        server.addInstalledAppForValidation(appname + "pl");

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
            server.stopServer("CWNEN0070W");
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
    public void testThirdpartyJerseyClient_ParentFirst() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(targetpf, "testFlowProgram_ClientStandalone", p, "[Basic Resource]:alex");
    }

    @Test
    public void testThirdpartyJerseyClient_ParentLast() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(targetpl, "testFlowProgram_ClientStandalone", p, "[Basic Resource]:alex");
    }
}
