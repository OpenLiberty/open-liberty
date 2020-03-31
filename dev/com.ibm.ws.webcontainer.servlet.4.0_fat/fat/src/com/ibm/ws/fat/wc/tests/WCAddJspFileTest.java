/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * All Servlet 4.0 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
public class WCAddJspFileTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCAddJspFileTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_addJspFileServer");

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
        LOG.info("Setup : add TestAddJspFile to the server if not already present.");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestAddJspFile.ear", false,
                                                  "TestAddJspFile.war", true, null, false, "testaddjspfile.war.listeners");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestAddJspFile", WCAddJspFileTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().stopServer();
    }

    /**
     * Request a simple servlet.
     *
     * @throws Exception
     */
    @Test
    public void testJSPOne() throws Exception {
        this.verifyResponse("/TestAddJspFile/jsp1", "Welcome to jsp one.jsp");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJSPOneDirect() throws Exception {
        this.verifyResponse("/TestAddJspFile/addJsp/one.jsp", "Welcome to jsp one.jsp");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJSPTwo() throws Exception {
        this.verifyResponse("/TestAddJspFile/jsp2", "Welcome to jsp two.jsp");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJSPDefinedInWebXml() throws Exception {
        this.verifyResponse("/TestAddJspFile/webxmljsp", "Welcome to jsp webxml.jsp");
    }

    @Test
    public void testJSPPartiallyDefinedInWebXml() throws Exception {

        this.verifyResponse("/TestAddJspFile/webxmlpartialone", "Welcome to jsp webxmlpartialone.jsp");
    }

    @Test
    public void testJSPMultipleMappingPartiallyDefinedInWebXml() throws Exception {
        this.verifyResponse("/TestAddJspFile/webxmlpartialtwo", "Welcome to jsp webxmlpartialtwo.jsp");
        this.verifyResponse("/TestAddJspFile/webxmlpartialthree", "Welcome to jsp webxmlpartialtwo.jsp");
        this.verifyResponse("/TestAddJspFile/webxmlpartialfour", "Welcome to jsp webxmlpartialtwo.jsp");

    }

    @Test
    public void testForCorrectExceptions() throws Exception {
        // Messages will come out during server start so should be there when
        // this test runs
        List<String> messages = SHARED_SERVER.getLibertyServer().findStringsInLogs("TEST.*: AddJspContextListener registration of a jsp with servletname");
        boolean failFound = false;
        for (String message : messages) {
            LOG.info("Test message found in logs : " + message);
            failFound = failFound || message.contains("FAILED");
        }
        assertFalse("Test Failed : Failure message found in log", failFound);
        assertTrue("Test Failed : Expected 2 messages but got : " + messages.size(), messages.size() == 2);
    }

}