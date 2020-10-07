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
/*
 * Testcase for issues:
 * https://github.com/OpenLiberty/open-liberty/issues/9386
 */
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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * 1. Test the dynamic add servlet after the dynamic filter, which adds a servlet-filter name mapping for that servlet, to make sure no NPE.
 * Also verify that the filter mapping flow is not breaking (i.e continue to the next filter mapping during filter chain build up)
 *
 * 2. If (1) is successful, verify the new WARNING message:
 *
 * [WARNING ] CWWWC0002W: No servlet definition is found for the servlet name {0} for filter mapping {1}
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class WCServletContainerInitializerFilterServletNameMappingTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServletContainerInitializerFilterServletNameMappingTest.class.getName());
    private static final String WAR_NAME = "SCIFilterServletNameMapping.war";
    private static final String JAR_NAME = "SCIFilterServletNameMapping.jar";
    private static final String JAR_RESOURCE = "testsci.jar.servletsfilters";

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add " + WAR_NAME + " to the server if not already present.");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(),
                                                  null, false,
                                                  WAR_NAME, false,
                                                  JAR_NAME, true,
                                                  JAR_RESOURCE);

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("SCIFilterServletNameMapping", WCServletContainerInitializerFilterServletNameMappingTest.class.getName(),
                                            SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        SHARED_SERVER.getLibertyServer().stopServer("CWWWC0002W", "CWWWC0001W");
    }

    /**
     * Request will trigger a filter, which adds "SharedFilter.doFilter" to response, then continue to a servlet which adds " | Hello World from TestServlet2"
     * CWWWC0002W will happen during this request
     *
     * @throws Exception
     */
    @Test
    public void testDynamicAddServletAfterFilter() throws Exception {
        LOG.info("Testing testDynamicAddServletAfterFilter 1/2");
        try {
            this.verifyResponse("/SCIFilterServletNameMapping/Test2ServletFilterNameMapping", "SharedFilter.doFilter | Hello World from TestServlet2");
        } catch (Exception e) {
            LOG.info("Testing testDynamicAddServletAfterFilter failed.  Skip the checking for Warning message");
            throw e;
        }

        LOG.info("Testing testDynamicAddServletAfterFilter 2/2 : Checking the expected Warning message CWWWC0002W.");
        org.junit.Assert.assertTrue("CWWWC0002W: No servlet definition is found for the servlet name",
                                    !SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWWC0002W.*").isEmpty());
    }
}
