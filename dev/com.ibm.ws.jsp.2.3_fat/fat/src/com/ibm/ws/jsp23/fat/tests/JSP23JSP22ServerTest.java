/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * Tests to execute on the jsp23jsp22Server that use HttpUnit.
 */
@Mode(TestMode.FULL)
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSP23JSP22ServerTest {
    private static final Logger LOG = Logger.getLogger(JSP23JSP22ServerTest.class.getName());
    private static final String APP_NAME = "TestJspFeatureChange";

    @Server("jsp23jsp22Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server,
                                      APP_NAME + ".war");

        server.startServer(JSP23JSP22ServerTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        /**
         * Stopping the server before changing the server features
         * makes CWWKZ0014W to not happen.
         */
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

        //set it back to jsp-2.3
        List<String> jsp23Feature = new ArrayList<String>();
        jsp23Feature.add("jsp-2.3");
        server.changeFeatures(jsp23Feature);
    }

    /**
     * Test a JSP request with the jsp-2.3 feature enabled.
     * Then change the feature to jsp-2.2 and request the same JSP again.
     * The JSP should be recompiled.
     * The JSP file pulls the version out of it's generated code and displays it in the rendered page.
     *
     * @throws Exception
     */
    @Test
    public void testJspFeatureChange() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        LOG.info("Requesting JSP with jsp-2.3 feature enabled");

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "testJspFeatureChange.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response from a 2.3 compilation: " + response.getText());

        assertTrue("The response did not contain: JSP version: 2.3", response.getText().contains("JSP version: 2.3"));

        List<String> jsp22Feature = new ArrayList<String>();
        jsp22Feature.add("jsp-2.2");
        server.changeFeatures(jsp22Feature);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("TestJspFeatureChange"), true, new String[0]);

        LOG.info("Requesting JSP with jsp-2.2 feature enabled");

        response = wc.getResponse(request);

        LOG.info("Response from a 2.2 compilation: " + response.getText());

        assertTrue("The response did not contain: JSP version: 2.2", response.getText().contains("JSP version: 2.2"));

    }
}
