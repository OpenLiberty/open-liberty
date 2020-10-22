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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS20WithClientFeatureEnabledTest extends AbstractTest {

    @Server("jaxrs20.client.JAXRS20WithClientFeatureEnabledTest")
    public static LibertyServer server;

    private final static String appname = "jaxrs20withclientfeatureenabled";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.JAXRS20WithClientFeatureEnabledTest.client",
                                                       "com.ibm.ws.jaxrs20.client.JAXRS20WithClientFeatureEnabledTest.service");

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
    public void testNewClientBuilder_WithClientFeature() throws Exception {
        this.runTestOnServer(target, "testNewClientBuilder_WithClientFeature", null, "OK");
    }

    @Test
    public void testNewClient_WithClientFeature() throws Exception {
        this.runTestOnServer(target, "testNewClient_WithClientFeature", null, "OK");
    }

    @Test
    public void testNewWebTarget_WithClientFeature() throws Exception {
        this.runTestOnServer(target, "testNewWebTarget_WithClientFeature", null, "OK");
    }

    @Test
    public void testNewInvocationBuilder_WithClientFeature() throws Exception {
        this.runTestOnServer(target, "testNewInvocationBuilder_WithClientFeature", null, "OK");
    }

    @Test
    public void testNewInvocation_WithClientFeature() throws Exception {
        this.runTestOnServer(target, "testNewInvocation_WithClientFeature", null, "OK");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
    public void testFlowProgram_WithClientFeature() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testFlowProgram_WithClientFeature", p, "[Basic Resource]:alex");
    }

    //TODO: we should also migrate more jaxrs-1.1 cases here
}
