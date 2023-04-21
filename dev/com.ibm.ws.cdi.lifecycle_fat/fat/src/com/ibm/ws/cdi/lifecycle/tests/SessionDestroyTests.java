/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.tests;

import java.net.MalformedURLException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.lifecycle.apps.sessionDestroyWar.InvalidateServlet;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SessionDestroyTests {

    private static final String SERVER_NAME = "cdi12SessionInvalidationServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
                                                         EERepeatActions.EE10,
                                                         EERepeatActions.EE9,
                                                         EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String expectedResults = "session created: true session destroyed: true";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive sessionDestroyWar = ShrinkWrap.create(WebArchive.class, "SessionDestroy.war")
                                                 .addPackage(InvalidateServlet.class.getPackage())
                                                 .addAsWebInfResource(InvalidateServlet.class.getResource("beans.xml"), "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, sessionDestroyWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

    @Test
    public void testInvalidation() throws Exception {

        HttpUtils.getHttpResponseAsString(createURL("/InvalidateServlet"));
        String response = HttpUtils.getHttpResponseAsString(createURL("/ResultsServlet"));

        Assert.assertTrue("expected " + "Invalidate Session - " + expectedResults + " but saw " + response,
                          response.contains("Invalidate Session - " + expectedResults));

    }

    @Test
    public void testTimeout() throws Exception {

        // This test does not work using HttpUtils/HttpUrlConnection - I suspect the connection is not immediately closed
        WebBrowser wb = WebBrowserFactory.getInstance().createWebBrowser();
        wb.request(createURL("/TimeoutServlet"));
        Thread.sleep(3000);
        wb.request(createURL("/ResultsServlet")); //poke it a second time to ensure that the timeout is processed.
        WebResponse webResponse = wb.request(createURL("/ResultsServlet"));

        Assert.assertTrue("expected " + "Timeout Session - " + expectedResults + " but saw " + webResponse.getResponseBody(),
                          webResponse.getResponseBody().contains("Timeout Session - " + expectedResults));

    }

    private String createURL(String path) throws MalformedURLException {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/SessionDestroy" + path;
    }

}
