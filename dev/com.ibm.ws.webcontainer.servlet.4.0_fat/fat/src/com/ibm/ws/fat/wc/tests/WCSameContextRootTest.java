/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Starting applications with same context root - verify that the second starting app with same context root cannot
 * stop the first started app. Also the second app cannot start with the exception SRVE0164E "context.root.already.in.use"
 *
 * Note: server.xml uses the "startAfter" attribute to control the app startup order. Need that in order to have consistent behavior
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCSameContextRootTest {
    private static final Logger LOG = Logger.getLogger(WCSameContextRootTest.class.getName());
    private static final String APP1_NAME = "SameContextRootTestApp1";
    private static final String APP2_NAME = "SameContextRootTestApp2";

    @Server("servlet40_SameContextRootTestServer")
    public static LibertyServer server;

    @BeforeClass
    @ExpectedFFDC({ "com.ibm.ws.webcontainer.exception.WebAppNotLoadedException", "com.ibm.ws.container.service.state.StateChangeException" })
    public static void setUp() throws Exception {
        LOG.info("Setup 2 application with same context root: " + APP1_NAME + " and " + APP2_NAME);

        //put in /apps folder
        ShrinkHelper.defaultApp(server, APP1_NAME + ".war", "app1.same.contextroot");
        ShrinkHelper.defaultApp(server, APP2_NAME + ".war", "app2.same.contextroot");

        // We can't start the server like normal as there are expected application startup errors.
        // Instead we'll set the log name, start the server and won't validate the applications.
        // Then we'll validate the applications on our own.
        server.setConsoleLogName(WCSameContextRootTest.class.getSimpleName() + ".log");
        server.startServerAndValidate(true, true, false);

        // Validate the necessary application has started.
        server.validateAppLoaded(APP1_NAME);

        server.waitForMultipleStringsInLog(1, "SRVE0164E: Web Application SameContextRootTestApp2 uses the context root");
        server.waitForMultipleStringsInLog(1, "CWWKZ0002E: An exception occurred while starting the application SameContextRootTestApp2");

        // wait for the three ffdc
        //FFDC1015I: An FFDC Incident has been created: "com.ibm.ws.webcontainer.exception.WebAppNotLoadedException: Failed to load webapp: SRVE0164E: Web Application SameContextRootTestApp2 uses the context root /SameContextRootTest/*, which is already in use by Web Application SameContextRootTestApp1. Web Application SameContextRootTestApp2 will not be loaded. com.ibm.ws.webcontainer.osgi.WebContainer startModule" at ffdc_23.09.14_11.05.51.1.log
        //FFDC1015I: An FFDC Incident has been created: "com.ibm.ws.webcontainer.exception.WebAppNotLoadedException: Failed to load webapp: SRVE0164E: Web Application SameContextRootTestApp2 uses the context root /SameContextRootTest/*, which is already in use by Web Application SameContextRootTestApp1. Web Application SameContextRootTestApp2 will not be loaded. com.ibm.ws.webcontainer.osgi.WebContainer startModule" at ffdc_23.09.14_11.05.51.1.log
        //FFDC1015I: An FFDC Incident has been created: "com.ibm.ws.container.service.state.StateChangeException: com.ibm.ws.webcontainer.exception.WebAppNotLoadedException: Failed to load webapp: SRVE0164E: Web Application SameContextRootTestApp2 uses the context root /SameContextRootTest/*, which is already in use by Web Application SameContextRootTestApp1. Web Application SameContextRootTestApp2 will not be loaded. com.ibm.ws.app.manager.module.internal.ModuleHandlerBase 102" at ffdc_23.09.14_11.05.51.2.log
        server.waitForMultipleStringsInLog(3, "FFDC1015I");

        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("tearDown : stop server");

        if (server != null && server.isStarted()) {
            /*
             * Expecting context root already in use and starting app error during the startModule
             *
             * SRVE0164E: Web Application SameContextRootTestApp2 uses the context root /SameContextRootTest/*, which is already in use by Web Application SameContextRootTestApp1.
             * CWWKZ0002E: An exception occurred while starting the application SameContextRootTestApp2.
             */
            server.stopServer("SRVE0164E:.*", "CWWKZ0002E:.*");
        }
    }

    /**
     * Send request and expect a simple response. This verifies that App1 is not stopped by App2 for having same context root.
     *
     * SRVE0164E will be verified during the server stop
     */
    @Test
    public void testSimpleRequest() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/SameContextRootTest/HelloServlet";
        String expectedResponse = "Hello World";

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            }
        }
    }
}
