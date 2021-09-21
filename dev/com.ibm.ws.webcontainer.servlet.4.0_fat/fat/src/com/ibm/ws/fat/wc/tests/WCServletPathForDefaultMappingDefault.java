/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
 * This is a test class for the servlet-4.0 feature to test the behavior when servletPathForDefaultMapping
 * is true. There is no need to set the property in the server.xml because the value is true by default
 * for the servlet-4.0 feature.
 *
 */
@RunWith(FATRunner.class)
public class WCServletPathForDefaultMappingDefault {

    private static final Logger LOG = Logger.getLogger(WCServletPathForDefaultMappingDefault.class.getName());
    private static final String APP_NAME = "ServletPathDefaultMapping";

    @Server("servlet40_ServletPathForDefaultMapping_Default")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add ServletPathDefaultMapping.war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "servletpathdefaultmapping.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCServletPathForDefaultMappingDefault.class.getSimpleName() + ".log");
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
     * Test to ensure that the default behavior when using the servlet-4.0 feature is
     * that a request to the default servlet will result in:
     *
     * servlet path = / + requestURI - context path
     * path info = null
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_Default() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME, "ServletPath = / PathInfo = null");
    }

    /**
     * Test to ensure that the default behavior when using the servlet-4.0 feature is
     * that a request to the default servlet will result in:
     *
     * servlet path = / + requestURI - context path
     * path info = null
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_Default_2() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/index.html", "ServletPath = /index.html PathInfo = null");
    }
}
