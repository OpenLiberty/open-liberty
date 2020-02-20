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

import java.util.ArrayList;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Servlet 4.0 Clarification Clarification 2. getAttribute,
 * getInitParameter return NPE if name is null Clarification 3. setAttribute,
 * setInitParameter return NPE if name is null
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServletClarificationTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServletClarificationTest.class.getName());

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

    @Before
    public void before() {
        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0400E:.*");
        SHARED_SERVER.getLibertyServer().addIgnoredErrors(expectedErrors);

    }

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestSertvlet40 app to server if not already present");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestServlet40.ear", true,
                                                  "TestServlet40.war", true, "TestServlet40.jar", true, "testservlet40.war.servlets",
                                                  "testservlet40.war.listeners", "testservlet40.jar.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestServlet40", WCServletClarificationTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().stopServer();
    }

    protected String parseResponse(WebResponse wr, String beginText, String endText) {
        String s;
        String body = wr.getResponseBody();
        int beginTextIndex = body.indexOf(beginText);
        if (beginTextIndex < 0)
            return "begin text, " + beginText + ", not found";
        int endTextIndex = body.indexOf(endText, beginTextIndex);
        if (endTextIndex < 0)
            return "end text, " + endText + ", not found";
        s = body.substring(beginTextIndex + beginText.length(), endTextIndex);
        return s;
    }

    @Test
    public void test_GetAttribute_Null_Name() throws Exception {
        this.verifyResponse("/TestServlet40/ServletClarification?TestNullName=getAttribute",
                            "Caught expected NPE, PASS");
    }

    @Test
    public void test_GetInitParameter_Null_Name() throws Exception {
        this.verifyResponse("/TestServlet40/ServletClarification?TestNullName=getInitParameter",
                            "Caught expected NPE, PASS");
    }

    @Test
    public void test_SetAttribute_Null_Name() throws Exception {
        this.verifyResponse("/TestServlet40/ServletClarification?TestNullName=setAttribute",
                            "Caught expected NPE, PASS");
    }

    @Test
    public void test_SetInitParameter_Null_Name() throws Exception {
        this.verifyResponse("/TestServlet40/ServletClarification?TestNullName=setInitParameter",
                            "Caught expected NPE, PASS");
    }

    /*
     * test valid set and get attribute name
     */
    @Test
    public void test_SetGetValidAttribute_Name() throws Exception {
        this.verifyResponse("/TestServlet40/ServletClarification?TestNullName=setGetValidAttribute",
                            "setAttribute and getAttribute, PASS");
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