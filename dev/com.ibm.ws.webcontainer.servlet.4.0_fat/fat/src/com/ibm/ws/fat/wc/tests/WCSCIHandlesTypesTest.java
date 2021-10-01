/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This tests the functionality of the
 * ServletContainerInitializer.onStartup(Set<Class<?>> c, ServletContext ctx) API.
 *
 * @See https://github.com/OpenLiberty/open-liberty/issues/16598
 *
 */
@RunWith(FATRunner.class)
public class WCSCIHandlesTypesTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    private static final String APP_NAME = "TestHandlesTypesClasses";

    @Server("servlet40_excludeAllHandledTypesClasses")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestHandlesTypesClasses to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "testhandlestypesclasses.examples", "testhandlestypesclasses.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCSCIHandlesTypesTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test to verify that the correct set of Classes is passed via
     * ServletContainerInitializer.onStartup(Set<Class<?>> c, ServletContext ctx)
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_ServletContainerInitializer_HandlesTypes_Classes() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/GetMappingTestServletSlashStar", "PASS");
    }

}
