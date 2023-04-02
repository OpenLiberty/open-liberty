/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.pages31.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import io.openliberty.pages31.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;



/**
 *  Smoke Tests for JSTL EE10 Tags
 */
@RunWith(FATRunner.class)
public class UnknownELIdentifiersTest {
    private static final String APP_NAME_1 = "ErrorELPageDirective";

    private static final String APP_NAME_2 = "ErrorELPropertyGroup";

    private static final String APP_NAME_3 = "ErrorELPropertyGroupOverride";

    private static final String APP_NAME_4 = "ErrorELTagDirective";

    private static final Logger LOG = Logger.getLogger(UnknownELIdentifiersTest.class.getName());

    @Server("pagesServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME_1 + ".war");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_2 + ".war");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_3 + ".war");
        ShrinkHelper.defaultDropinApp(server, APP_NAME_4 + ".war");
        server.startServer(UnknownELIdentifiersTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // Expected: SRVE0777E: Exception thrown by application class 'jakarta.servlet.jsp.el.NotFoundELResolver.getValue:68'
            // SRVE0315E: An exception occurred: java.lang.Throwable: jakarta.el.PropertyNotFoundException: Unknown identifier [test]
            server.stopServer("SRVE0777E","SRVE0315E");
        }
    }

    /**
     * Basic Test for a subset of the Core Tags
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testUnidentifiedExpression1() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME_1, "no-error.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Expected text not found!", response.getText().contains("No Exception should be thrown:  here. "));
    }

    @ExpectedFFDC(value = {"jakarta.el.PropertyNotFoundException"})
    @Test
    public void testUnidentifiedExpressionWithPageDirective() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME_1, "test.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Exception expected, but not found!", response.getText().contains("jakarta.el.PropertyNotFoundException"));

    }

    @Test
    public void testUnidentifiedExpression2() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME_2, "no-error.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Expected text not found!", response.getText().contains("No Exception should be thrown:  here. "));
    }

    @ExpectedFFDC(value = {"jakarta.el.PropertyNotFoundException"})
    @Test
    public void testUnidentifiedExpressionWithJspPropertyGroup() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME_2, "test.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Exception expected, but not found!", response.getText().contains("jakarta.el.PropertyNotFoundException"));

    }

    @Test
    public void testUnidentifiedExpressionWithJspPropertyGroupOverride() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME_3, "test-no-error.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Exception expected, but not found!", response.getText().contains("Exception should not be thrown:  here"));

    }

    // @ExpectedFFDC(value = {"jakarta.el.PropertyNotFoundException"})
    @Test
    public void testUnidentifiedExpressionWithTagDirectiveTrue() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME_4, "test-true.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Exception expected, but not found!", response.getText().contains("jakarta.el.PropertyNotFoundException"));

    }

    @Test
    public void testUnidentifiedExpressionWithTagDirectiveFalse() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
    
        String url = JSPUtils.createHttpUrlString(server, APP_NAME_4, "test-false.jsp");
        LOG.info("url: " + url);
    
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
    
        assertTrue("Expected text not found!", response.getText().contains("No exception expected  here"));
    
    }


}
