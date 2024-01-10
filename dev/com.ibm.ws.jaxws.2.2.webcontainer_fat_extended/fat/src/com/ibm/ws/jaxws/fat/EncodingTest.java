/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.test.wsr.test.servlet.EncodingTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class EncodingTest extends FATServletClient {

    private static final String APP_NAME = "encodingApp";

    @Server("EncodingTestServer")
    @TestServlet(servlet = EncodingTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.jaxws.test.wsr.server",
                                      "com.ibm.ws.jaxws.test.wsr.test.servlet",
                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                      "com.ibm.ws.jaxws.fat.util");
        server.startServer();

        assertNotNull("Application " + APP_NAME + " does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));

    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }
}
