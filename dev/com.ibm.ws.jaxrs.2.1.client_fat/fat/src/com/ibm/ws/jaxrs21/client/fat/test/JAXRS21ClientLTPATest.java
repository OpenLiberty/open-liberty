/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS21ClientLTPATest extends JAXRS21AbstractTest {
    @Server("jaxrs21.client.JAXRS21ClientLTPATest")
    public static LibertyServer server;

    private static final String clientltpawar = "jaxrs21clientltpa";

    private final static String target = "jaxrs21clientltpa/JAXRS21ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, clientltpawar,
                                      "com.ibm.ws.jaxrs21.client.JAXRS21ClientLTPA.client",
                                      "com.ibm.ws.jaxrs21.client.JAXRS21ClientLTPA.service");

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
    public void testClientLtpaHander_ClientNoTokenWithSSL() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "jordan");
        this.runTestOnServer(target, "testClientLtpaHander_ClientNoTokenWithSSL", p, "[Basic Resource]:jordan");
    }
}
