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

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE11_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * 
 * Verified the new JSP SPI methods work. Added in PR 26892.
 * Test ensure the global tlds are picked up with the overriden URIs.
 * 
 * Test Bundle is not transformed, so we skipp EE9+ features
 */
@SkipForRepeat({"CDI-2.0",EE9_FEATURES,EE10_FEATURES,EE11_FEATURES})
@RunWith(FATRunner.class)
public class JSPGlobalTLDTest {
    private static final Logger LOG = Logger.getLogger(JSPGlobalTLDTest.class.getName());

    private static final String OLGH26891_APP_NAME = "OLGH26891";

    @Server("globalTLDServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, OLGH26891_APP_NAME + ".war");

        server.startServer(JSPGlobalTLDTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testGlobalTLDs_new_constructor() throws Exception {
        String url = JSPUtils.createHttpUrlString(server, OLGH26891_APP_NAME, "testGlobalTLDs-new-constructor.jsp");
        LOG.info("url: " + url);

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText()); 

        String expected = "***/WEB-INF/tld/test1.tld used!***";

        assertTrue("The response did not contain: " + expected, response.getText().contains(expected));

    }

    @Test
    public void testGlobalTLDs_old_constructor() throws Exception {
        String url = JSPUtils.createHttpUrlString(server, OLGH26891_APP_NAME, "testGlobalTLDs-old-constructor.jsp");
        LOG.info("url: " + url);

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText()); 

        String expected = "***io.test.one.tld***";

        assertTrue("The response did not contain: " + expected, response.getText().contains(expected));
    }
}
