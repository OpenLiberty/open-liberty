/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.fat.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.FATServletClient;
import com.ibm.ws.jaxrs21.client.threadleak.client.JAXRS21ClientTestServlet;

@RunWith(FATRunner.class)
@SkipForRepeat({SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.EE11_FEATURES})
public class JAXRS21ClientThreadLeakTest extends FATServletClient {

    private static final String appName = "jaxrs21clientthreadleak";

    @Server("jaxrs21.client.JAXRS21ClientThreadLeakTest")
    @TestServlet(servlet = JAXRS21ClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, appName,
                                      "com.ibm.ws.jaxrs21.client.threadleak.client",
                                      "com.ibm.ws.jaxrs21.client.threadleak.server");

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
    public void beforeTest() {}

    @After
    public void afterTest() {}
}
