/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import componenttest.topology.impl.LibertyServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;


/**
 *  There is debug support for JSP via the "-Dwas.debug.mode=true" JVM Option. Relates to a "source map" (SMAP).
 * 
 *  This verifies that JSPs compile with the option enabled. 
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@Mode(TestMode.FULL)
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPDebugSupport {
    private static final String APP_NAME = "HelloWorld";

    private static final Logger LOG = Logger.getLogger(JSPDebugSupport.class.getName());

    @Server("debugSupportServer")
    public static LibertyServer server;
   
    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(JSPDebugSupport.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     *  Hits index.jsp and verifies the page rendered without any exceptions. 
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testDebugSupport() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());


        assertTrue("Compilation may have failed!", response.getText().contains("Testing a Scriptlet"));
    }

}
