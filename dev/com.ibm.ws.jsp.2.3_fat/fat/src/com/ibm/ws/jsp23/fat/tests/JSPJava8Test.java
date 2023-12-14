/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import java.util.Collections;
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
import componenttest.topology.impl.LibertyServer;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
/**
 * JSP 2.3 tests which use Java 1.8 specific features.
 *
 * Tests must only run when Java 1.8 or later is in use.
 *
 * Tests that just need to drive a simple request using our WebBrowser object can be placed in this class.
 *
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPJava8Test {
    private static final String APP_NAME = "TestJSPWithJava8";
    private static final Logger LOG = Logger.getLogger(JSPJava8Test.class.getName());

    @Server("jspJava8Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(JSPJava8Test.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Simple test for Index.jsp
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    public void testJava8JSP() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: onetwothreefour", response.getText().contains("onetwothreefour"));
    }

    /**
     * Same test as testJava8JSP, but using the runtime JDK (via JSP's useJDKCompiler option rather than the default Eclipse Compiler for Java (ECJ))
     *
     * https://openliberty.io/docs/latest/reference/config/jspEngine.html
     * 
     * @throws Exception if something goes horribly wrong
     *                
     */
    @Test
    public void testJava8viaUseJDKCompiler() throws Exception {

        ServerConfiguration configuration = server.getServerConfiguration();
        configuration.getJspEngine().setUseJDKCompiler(true);
        LOG.info("New server configuration used: " + configuration);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.restartApplication(APP_NAME);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*TestJSPWithJava8.*");

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: onetwothreefour", response.getText().contains("onetwothreefour"));
    }
}
