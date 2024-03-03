/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.pages40.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.pages40.fat.JSPUtils;

/**
 * Various tests for Jakarta Pages 4.0.
 */
@RunWith(FATRunner.class)
public class Pages40ChangesTest {
    private static final String APP_NAME = "Pages40Changes";

    private static final Logger LOG = Logger.getLogger(Pages40ChangesTest.class.getName());

    @Server("pages40Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
        server.startServer(Pages40ChangesTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // SRVE8115W, SRVE8094W caused by exceptions thrown by the jsp:plugin tests
            server.stopServer("SRVE8115W", "SRVE8094W");
        }
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void verifyisThreadSafeIsRemoved() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "isThreadSafe.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("jsp:plugin should throw an error", response.getText().contains("JSPG0024E: Unknown directive isThreadSafe"));
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void verifyisJspPluginIsRemoved() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "jspPlugin.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("jsp:plugin should throw an error",
                   response.getText().contains("CWWJS0005E: The jsp:plugin element was removed in Pages 4.0 and therefore is no longer supported."));
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void verifyisJspParamsIsRemoved() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "jspParams.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("jsp:params should throw an error",
                   response.getText().contains("CWWJS0005E: The jsp:params element was removed in Pages 4.0 and therefore is no longer supported."));
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void verifyisJspFallbackIsRemoved() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "jspFallback.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("jsp:fallback should throw an error",
                   response.getText().contains("CWWJS0005E: The jsp:fallback element was removed in Pages 4.0 and therefore is no longer supported."));
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void verifyNewErrorDataValues() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "testQueryStringonError.jsp?test=true");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Query string not displayed on the error page!", response.getText().contains("queryString: test=true"));

        assertTrue("Method not displayed on the error page!", response.getText().contains("errorMethod: GET"));
    }
}
