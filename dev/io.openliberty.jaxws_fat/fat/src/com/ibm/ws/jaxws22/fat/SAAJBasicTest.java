/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat;

import static componenttest.annotation.SkipForRepeat.EE9_OR_LATER_FEATURES;
import static org.junit.Assert.assertNotNull;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import fats.cxf.jaxws22.saaj.client.SAAJTestServlet;

//fats.cxf.jaxws22.saaj.client

/**
 * A basic test for SAAJ implementation in Liberty 
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat({ EE9_OR_LATER_FEATURES })
public class SAAJBasicTest extends FATServletClient {

    private static final String APP_NAME = "saajtest";

    @Server("com.ibm.ws.jaxws22.saaj_fat")
    @TestServlet(servlet = SAAJTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "fats.cxf.jaxws22.saaj.client");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer();
        System.out.println("Starting Server");

        assertNotNull("Application " + APP_NAME + " does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));

        return;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}