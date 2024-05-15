/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jsp23.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**

 *
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@MaximumJavaLevel(javaLevel = 17)
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JSPJava7Test {
    
    private static final String APP_NAME = "TestJSPWithJava8"; // Using Java 8 Test App to Verify it fails with Java 7 Source

    private static final Logger LOG = Logger.getLogger(JSPJava7Test.class.getName());

    @Server("jspJava7Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(JSPJava7Test.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // SRVE8115W: WARNING: Cannot set header. Response already committed.
            // SRVE8094W: WARNING: Cannot set header. 
            server.stopServer("SRVE8115W", "SRVE8094W", "CWWKE0921W", "CWWKE0912W");
        }
    }

    /*
     * Ensure JdkSourceLevel works by testing option 17 (Java 7).
     * Lambda were added in Java 8, so the JSP is expected to fail.
     */
    @Test
    @AllowedFFDC("java.security.PrivilegedActionException") // Occurs in EE10 and lower
    public void testJdkSourceLevel17() throws Exception {

        ServerConfiguration configuration = server.getServerConfiguration();
        configuration.getJspEngine().setJdkSourceLevel("17");
        configuration.getJspEngine().setUseJDKCompiler(false);
        LOG.info("New server configuration used: " + configuration);

        server.updateServerConfiguration(configuration);
        server.restartServer();

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());

        assertTrue("Compilation error message not found!", response.getText().contains("Lambda expressions are allowed only at source level 1.8 or above")); 
    }

    /*
     * Ensure JdkSourceLevel works by testing option 17 (Java 7).
     * Lambda were added in Java 8, so the JSP is expected to fail.
     */
    @Test
    @AllowedFFDC("java.security.PrivilegedActionException") // Occurs in EE10 and lower
    public void testJdkSourceLevel17withPreCompile() throws Exception {

        ServerConfiguration configuration = server.getServerConfiguration();
        configuration.getJspEngine().setJdkSourceLevel("17");
        configuration.getJspEngine().setUseJDKCompiler(false);
        configuration.getJspEngine().setPrepareJSPs("0");
        LOG.info("New server configuration used: " + configuration);

        server.updateServerConfiguration(configuration);
        server.stopServer("SRVE8115W","SRVE8094W");
        server.startServer();

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());

        assertTrue("Compilation error message not found!", response.getText().contains("Lambda expressions are allowed only at source level 1.8 or above")); 
    }

    /*
     * Ensure JdkSourceLevel works by testing option 17 (Java 7).
     * Lambda were added in Java 8, so the JSP is expected to fail.
     */
    @Test
    @AllowedFFDC("java.security.PrivilegedActionException") // Occurs in EE10 and lower
    public void testJdkSourceLevel17withPreCompileAndUseJDKCompiler() throws Exception {

        ServerConfiguration configuration = server.getServerConfiguration();
        configuration.getJspEngine().setJdkSourceLevel("17");
        configuration.getJspEngine().setUseJDKCompiler(true);
        configuration.getJspEngine().setPrepareJSPs("0");
        LOG.info("New server configuration used: " + configuration);

        server.updateServerConfiguration(configuration);
        server.stopServer("SRVE8115W","SRVE8094W");
        server.startServer();

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());

        assertTrue("Compilation error message not found!", response.getText().contains("use -source 8 or higher to enable lambda expressions")); 
    }
}
