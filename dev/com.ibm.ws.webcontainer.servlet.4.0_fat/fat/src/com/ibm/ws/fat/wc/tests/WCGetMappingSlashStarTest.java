/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;

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
public class WCGetMappingSlashStarTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestGetMappingSlashStar to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestGetMappingSlashStar.war", false,
                                                  "testgetmappingslashstar.war.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestGetMappingSlashStar", WCGetMappingTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().stopServer();
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
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMappingSlashStar/firstTestPath/secondTestPath",
                                     "ServletMapping values: mappingMatch: PATH matchValue: firstTestPath/secondTestPath pattern: /* servletName: GetMappingTestServletSlashStar");
    }

}
