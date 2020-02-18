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
 * Note this is a basic test to ensure newPushBuilder() returns null for non H2 requests.
 * A comprehensive test of the pushbuilder api is included in the Servlet 4.0 unit tests.
 * A comprehensive test of pushbuilder functionality for H2 requests is included in the transport FAT bucket.
 */
@RunWith(FATRunner.class)
public class WCPushBuilderTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCPushBuilderTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestServlet40.ear", true,
                                                  "TestServlet40.war", true, "TestServlet40.jar", true, "testservlet40.war.servlets",
                                                  "testservlet40.war.listeners", "testservlet40.jar.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestServlet40", WCPushBuilderTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().stopServer();
    }

    @Test
    public void testPushBuilderAPI() throws Exception {
        String[] expectedMessages = { "PASS" };
        String[] unExpectedMessages = { "FAIL" };

        this.verifyResponse("/TestServlet40/PushBuilderAPIServlet", expectedMessages, unExpectedMessages);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

}
