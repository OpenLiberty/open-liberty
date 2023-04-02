/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
/*
 * Testcase for issues:
 * https://github.com/OpenLiberty/open-liberty/issues/9386
 */
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
 * 1. Test the dynamic add servlet after the dynamic filter, which adds a servlet-filter name mapping for that servlet, to make sure no NPE.
 * Also verify that the filter mapping flow is not breaking (i.e continue to the next filter mapping during filter chain build up)
 *
 * 2. If (1) is successful, verify the new WARNING message:
 *
 * [WARNING ] CWWWC0002W: No servlet definition is found for the servlet name {0} for filter mapping {1}
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class WCServletContainerInitializerFilterServletNameMappingTest {

    private static final Logger LOG = Logger.getLogger(WCServletContainerInitializerFilterServletNameMappingTest.class.getName());
    private static final String WAR_NAME = "SCIFilterServletNameMapping.war";
    private static final String JAR_NAME = "SCIFilterServletNameMapping.jar";
    private static final String JAR_RESOURCE = "testsci.jar.servletsfilters";

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add " + WAR_NAME + " to the server if not already present.");

        // Create the JAR
        JavaArchive sciFilterServletNameMappingJar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        sciFilterServletNameMappingJar.addPackage(JAR_RESOURCE);
        ShrinkHelper.addDirectory(sciFilterServletNameMappingJar, "test-applications/" + JAR_NAME + "/resources");

        // Create the WAR
        WebArchive sciFilterServletNameMappingWar = ShrinkWrap.create(WebArchive.class, WAR_NAME);
        sciFilterServletNameMappingWar.addAsLibrary(sciFilterServletNameMappingJar);

        ShrinkHelper.exportToServer(server, "dropins", sciFilterServletNameMappingWar);

        LOG.info("Setup : complete, ready for Tests");
        server.startServer(WCServletContainerInitializerFilterServletNameMappingTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer("CWWWC0002W", "CWWWC0001W");
        }
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
            HttpUtils.findStringInReadyUrl(server, "SCIFilterServletNameMapping/Test2ServletFilterNameMapping",
                                           "SharedFilter.doFilter | Hello World from TestServlet2");
        } catch (Exception e) {
            LOG.info("Testing testDynamicAddServletAfterFilter failed.  Skip the checking for Warning message");
            throw e;
        }

        LOG.info("Testing testDynamicAddServletAfterFilter 2/2 : Checking the expected Warning message CWWWC0002W.");
        org.junit.Assert.assertTrue("CWWWC0002W: No servlet definition is found for the servlet name",
                                    !server.waitForStringInLog("CWWWC0002W.*").isEmpty());
    }
}
