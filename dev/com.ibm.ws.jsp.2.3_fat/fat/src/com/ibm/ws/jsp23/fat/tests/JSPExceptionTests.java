/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertEquals;
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

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test exception behavior from JSP
 */

@SkipForRepeat("CDI-2.0")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSPExceptionTests {
    private static final Logger LOG = Logger.getLogger(JSPExceptionTests.class.getName());
    private static final String APP_NAME = "TestServlet";

    @Server("jspServletExceptionWrapServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war"); 
        server.startServer(JSPExceptionTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server, with expected exception
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0777E");
        }
    }

    /**
     * Test Exception in log following exception in dispatch forward
     * call with donotcloseoutputonforwardforservleterror=true
     *
     * @throws Exception
     *                       if something goes wrong
     */
    @Test
    public void testServletException() throws Exception {
        String verifyText1 = "java.lang.Exception: Exception from JSP";
        String verifyText2 = "com.ibm.websphere.servlet.error.ServletErrorReport: java.lang.Exception: Exception from JSP";
        server.setMarkToEndOfLog();

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "exception.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());
        //exception.jsp throws 'Exception',
        //   response code is 500
        //   both response and messages.log have the verify messages
        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());
        assertTrue("Response should contain " + verifyText1 + ".",
                   response.getText().contains(verifyText1));
        assertTrue("Log should contain " + verifyText2 + ".",
                   null != server.waitForStringInLogUsingMark(verifyText2));
    }

    /**
     * Test that previously truncated text appears in response from
     * forwarded exception with donotcloseoutputonforwardforservleterror=true
     *
     * @throws Exception
     *                       if something goes wrong
     */
    @Test
    public void testFwdException() throws Exception {
        String verifyText1 = "more text following flush";

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "testfwdexc.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());
        //after forward, exception.jsp throws 'Exception', testfwdexc catches and creates more output
        //   response code is 200
        //   response contains text that previously would have been truncated
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("Response should contain " + verifyText1 + ".",
                   response.getText().contains(verifyText1));
    }
}
