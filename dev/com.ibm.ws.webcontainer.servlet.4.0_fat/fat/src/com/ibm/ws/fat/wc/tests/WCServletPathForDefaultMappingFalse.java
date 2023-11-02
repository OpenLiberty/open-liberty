/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
 * This is a test class for the servlet-4.0 feature to test the behavior when servletPathForDefaultMapping
 * is false. Setting servletPathForDefaultMapping to false is the testing the non default behavior.
 *
 */
@RunWith(FATRunner.class)
public class WCServletPathForDefaultMappingFalse {

    private static final Logger LOG = Logger.getLogger(WCServletPathForDefaultMappingFalse.class.getName());
    private static final String APP_NAME = "ServletPathDefaultMapping";

    @Server("servlet40_ServletPathForDefaultMapping_False")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add ServletPathDefaultMapping.war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "servletpathdefaultmapping.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCServletPathForDefaultMappingFalse.class.getSimpleName() + ".log");
        server.waitForStringInLogUsingMark("CWWKO0219I*");
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
     * Test to ensure that if we set servletPathForDefaultMapping to false in the server.xml ( not the
     * default value for the servlet-4.0 feature) then a request to the default servlet will result in:
     *
     * servlet path = "" (empty string)
     * path info = / request URI - context path
     *
     * This is the incorrect behavior
     * according to the servlet specification and that is why we're making it the non default behavior
     * for the servlet-4.0 feature.
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_False() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME, "ServletPath =  PathInfo = /");
    }

    /**
     * Test to ensure that if we set servletPathForDefaultMapping to false in the server.xml ( not the
     * default value for the servlet-4.0 feature) then a request to the default servlet will result in:
     *
     * servlet path = "" (empty string)
     * path info = / request URI - context path
     *
     * This is the incorrect behavior
     * according to the servlet specification and that is why we're making it the non default behavior
     * for the servlet-4.0 feature.
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_False_2() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/index.html", "ServletPath =  PathInfo = /index.html");
    }
}
