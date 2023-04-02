/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test Servlet 4.0 Clarification Clarification 2. getAttribute,
 * getInitParameter return NPE if name is null Clarification 3. setAttribute,
 * setInitParameter return NPE if name is null
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServletClarificationTest {

    private static final Logger LOG = Logger.getLogger(WCServletClarificationTest.class.getName());

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestServlet40Clarifications app to server if not already present");

        ShrinkHelper.defaultDropinApp(server, "TestServlet40Clarifications.war", "servletclarifications.servlets");

        LOG.info("Setup : complete, ready for Tests");
        server.startServer(WCServletClarificationTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        /*
         * CWWWC0400E: An attribute name, or initial parameter name, is null.
         */
        if (server != null && server.isStarted()) {
            server.stopServer("CWWWC0400E");
        }
    }

    @Test
    public void test_GetAttribute_Null_Name() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40Clarifications/ServletClarification?TestNullName=getAttribute", "Caught expected NPE, PASS");
    }

    @Test
    public void test_GetInitParameter_Null_Name() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40Clarifications/ServletClarification?TestNullName=getInitParameter", "Caught expected NPE, PASS");
    }

    @Test
    public void test_SetAttribute_Null_Name() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40Clarifications/ServletClarification?TestNullName=setAttribute", "Caught expected NPE, PASS");
    }

    @Test
    public void test_SetInitParameter_Null_Name() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40Clarifications/ServletClarification?TestNullName=setInitParameter", "Caught expected NPE, PASS");
    }

    /*
     * test valid set and get attribute name
     */
    @Test
    public void test_SetGetValidAttribute_Name() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40Clarifications/ServletClarification?TestNullName=setGetValidAttribute", "setAttribute and getAttribute, PASS");
    }

    /*
     * @Test
     *
     * public void test_Valid_Name() throws Exception {
     * this.verifyResponse(
     * "/TestServlet40/ServletClarification?TestValidName=dummyValue", new
     * String[] { "TestValidName" }, new String[] { "FAILED" }); }
     */
}