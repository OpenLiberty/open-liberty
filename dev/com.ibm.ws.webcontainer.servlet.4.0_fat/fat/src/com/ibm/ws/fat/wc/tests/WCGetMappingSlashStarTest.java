/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * These are tests for the Servlet 4.0 HttpServletRequest.getMapping()
 * functionality.
 *
 * This expands the main WCGetMappingTest to test the /* mapping.
 * We can't use the WCGetMappingTest because it already has default servlet /
 * mapping which will be unreachable by this /* mapping.
 *
 */
@RunWith(FATRunner.class)
public class WCGetMappingSlashStarTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    private static final String APP_NAME = "TestGetMappingSlashStar";

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestGetMappingSlashStar to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "testgetmappingslashstar.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCGetMappingSlashStarTest.class.getSimpleName() + ".log");
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
     * Test to ensure that a request that uses a slash star mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * /* mapping is considered a special case of PATH where the path is just /
     * The mappingPattern = /*, mappingValue = URI minus the leading slash
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_SlashStarMapping() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/firstTestPath/secondTestPath",
                                       "ServletMapping values: mappingMatch: PATH matchValue: firstTestPath/secondTestPath pattern: /* servletName: GetMappingTestServletSlashStar");
    }

}
