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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
/**
 * JSP 2.3 tests which use Java 21 specific features.
 *
 * Tests must only run when Java 21 or later is in use.
 *
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 21)
@SkipForRepeat("CDI-2.0") // No need to run against cdi-2.0 since these tests don't use CDI at all.
@RunWith(FATRunner.class)
public class JSPJava21Test {
    private static final String APP_NAME = "TestJSPWithJava21";
    private static final Logger LOG = Logger.getLogger(JSPJava21Test.class.getName());

    @Server("jspJava21Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(JSPJava21Test.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Simple test for index.jsp. Page uses Java 21's instanceof pattern matching
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    public void testJava21JSP() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: success", response.getText().contains("getFirst success!"));
        assertTrue("The response did not contain: success", response.getText().contains("switch success!"));
    }

     /**
     * Same test as testJava21JSP, but using the runtime JDK (via JSP's useJDKCompiler option rather than the default Eclipse Compiler for Java (ECJ))
     *
     * https://openliberty.io/docs/latest/reference/config/jspEngine.html
     * 
     * @throws Exception if something goes horribly wrong
     *                
     */
    @Test
    public void testJava21viaUseJDKCompiler() throws Exception {

        ServerConfiguration configuration = server.getServerConfiguration();
        configuration.getJspEngine().setUseJDKCompiler(true);
        LOG.info("New server configuration used: " + configuration);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.restartApplication(APP_NAME);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*TestJSPWithJava21.*");

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: success", response.getText().contains("getFirst success!"));
        assertTrue("The response did not contain: success", response.getText().contains("switch success!"));
    }
}
