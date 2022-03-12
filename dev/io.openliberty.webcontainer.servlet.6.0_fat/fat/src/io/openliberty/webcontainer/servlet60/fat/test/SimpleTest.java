/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer.servlet60.fat.test;

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
 * Simple Test for Servlet 6.0
 *
 * TODO: This can be deleted once we have other tests in the Servlet 6.0 bucket this is
 * just a place holder so we have one test case that executes.
 */
@RunWith(FATRunner.class)
public class SimpleTest {

    private static final Logger LOG = Logger.getLogger(SimpleTest.class.getName());
    private static final String SIMPLE_TEST_APP_NAME = "SimpleTest";

    @Server("servlet60_simpleTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, SIMPLE_TEST_APP_NAME + ".war", "simpletest.servlets");

        server.startServer(SimpleTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Request a TestServlet.
     *
     * @throws Exception
     */
    @Test
    public void testTestServlet() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + SIMPLE_TEST_APP_NAME + "/TestServlet", "Hello from the TestServlet!!");
    }
}
