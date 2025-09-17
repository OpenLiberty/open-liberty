/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat;

import static org.junit.Assert.assertNotNull;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.special.characters.servlet.SpecialCharacterTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;


//fats.cxf.jaxws22.saaj.client

/**
 * A basic test for SAAJ implementation in Liberty 
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SpecialCharactersTest extends FATServletClient {

    private static final String APP_NAME = "specialcharacters";

    @Server("com.ibm.ws.jaxws22.specialcharacters_fat")
    @TestServlet(servlet = SpecialCharacterTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.special.characters.____.__or", "com.ibm.ws.jaxws.special.characters.____$__or",
                        "com.ibm.ws.jaxws.special.characters.servlet");

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