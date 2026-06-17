/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsp23.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test for NPE issue when fileServingEnabled is false and JSP code attempts 
 * to include an empty string path.
 * 
 * When request.getRequestDispatcher("") is called and fileServingEnabled is false,
 * the RequestDispatcher is null, which causes a NullPointerException in 
 * JspRuntimeLibrary.include().
 * 
 * Expected behavior: A ServletException should be thrown instead of NPE.
 * 
 * Related Issue: The JspRuntimeLibrary#include method may throw a NPE if 
 * RequestDispatcher is null.
 */
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JSPEmptyIncludePathTest {
    private static final String APP_NAME = "TestEmptyIncludePath";
    private static final Logger LOG = Logger.getLogger(JSPEmptyIncludePathTest.class.getName());

    @Server("emptyIncludePathServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(JSPEmptyIncludePathTest.class.getSimpleName() + ".log");
        server.waitForStringInLog("CWWKT0016I:.*" + APP_NAME + ".*");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        // SRVE0315E: An exception occurred
        // SRVE8115W: WARNING: Cannot set status.
        // SRVE0777E: Exception thrown by application class 
        // SRVE8094W: WARNING: Cannot set header.
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0315E", "SRVE8115W", "SRVE0777E", "SRVE8094W");
        }
    }

    /**
     * Test that when fileServingEnabled is false and JSP includes an empty string path,
     * a ServletException is thrown instead of NullPointerException.
     * 
     * @throws Exception if something goes wrong
     */
    @Test
    @ExpectedFFDC("javax.servlet.ServletException")
    public void testEmptyIncludePathWithFileServingDisabled() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "emptyInclude.jsp");
        LOG.info("Testing URL: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        
        int responseCode = response.getResponseCode();
        String responseText = response.getText();
        
        LOG.info("Response code: " + responseCode);
        LOG.info("Response text: " + responseText);

        // Verify we get an error response (500 Internal Server Error)
        assertTrue("Expected 500 status code for empty include path, but got: " + responseCode,
                   responseCode == 500);
    }

    /**
     * Test that when fileServingEnabled is false and JSP includes an empty string path
     * using flush=true, a ServletException is thrown instead of NullPointerException.
     * 
     * This tests the specific scenario mentioned in the issue with flush="true".
     * 
     * @throws Exception if something goes wrong
     */
    @Test
    @ExpectedFFDC("javax.servlet.ServletException")
    public void testEmptyIncludePathWithFlushTrue() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "emptyIncludeWithFlush.jsp");
        LOG.info("Testing URL: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        
        int responseCode = response.getResponseCode();
        String responseText = response.getText();
        
        LOG.info("Response code: " + responseCode);
        LOG.info("Response text: " + responseText);

        // Note: 200 is recieved is a flush occurs first (before the include) 
        // Main focus of the test is the ExpectedFFDC annotation to verify the correct exception is thrown
    }
}