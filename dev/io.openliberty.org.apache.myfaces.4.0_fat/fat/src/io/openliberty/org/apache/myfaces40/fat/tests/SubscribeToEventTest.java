/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Faces 4.0 modified UIComponent#subscribeToEvent() and UIComponent#getListenersForEventClass()
 * APIs to be more developer friendly.
 * This test verifies the API changes where correctly implemented.
 */
@RunWith(FATRunner.class)
public class SubscribeToEventTest {

    private static final String APP_NAME = "SubscribeToEventTest";

    @Server("faces40_subscribeToEventTest")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.subscribe");

        server.startServer(SimpleTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testSubscribeToEventBehaviorChange() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/subscribeToEventTest.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");
            assertEquals("subscribeToEvent method behaved incorrectly", expected, page.getBody().asText());
        }
    }

}
