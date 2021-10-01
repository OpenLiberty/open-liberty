/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
 * All Servlet 4.0 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
public class WCAddJspFileTest {

    private static final Logger LOG = Logger.getLogger(WCAddJspFileTest.class.getName());

    @Server("servlet40_addJspFileServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestAddJspFile to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, "TestAddJspFile.war", "testaddjspfile.listeners");

        LOG.info("Setup : complete, ready for Tests");
        server.startServer(WCAddJspFileTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Request a simple servlet.
     *
     * @throws Exception
     */
    @Test
    public void testJSPOne() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/jsp1", "Welcome to jsp one.jsp");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJSPOneDirect() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/addJsp/one.jsp", "Welcome to jsp one.jsp");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJSPTwo() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/jsp2", "Welcome to jsp two.jsp");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJSPDefinedInWebXml() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/webxmljsp", "Welcome to jsp webxml.jsp");
    }

    @Test
    public void testJSPPartiallyDefinedInWebXml() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/webxmlpartialone", "Welcome to jsp webxmlpartialone.jsp");
    }

    @Test
    public void testJSPMultipleMappingPartiallyDefinedInWebXml() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/webxmlpartialtwo", "Welcome to jsp webxmlpartialtwo.jsp");
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/webxmlpartialthree", "Welcome to jsp webxmlpartialtwo.jsp");
        HttpUtils.findStringInReadyUrl(server, "/TestAddJspFile/webxmlpartialfour", "Welcome to jsp webxmlpartialtwo.jsp");

    }

    @Test
    public void testForCorrectExceptions() throws Exception {
        // Messages will come out during server start so should be there when
        // this test runs
        List<String> messages = server.findStringsInLogs("TEST.*: AddJspContextListener registration of a jsp with servletname");
        boolean failFound = false;
        for (String message : messages) {
            LOG.info("Test message found in logs : " + message);
            failFound = failFound || message.contains("FAILED");
        }
        assertFalse("Test Failed : Failure message found in log", failFound);
        assertTrue("Test Failed : Expected 2 messages but got : " + messages.size(), messages.size() == 2);
    }
}