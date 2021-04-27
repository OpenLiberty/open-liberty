/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test Servlet 5 native jakarta.servlet (i.e not transformer)
 * Should only run in EE9.
 * The target app is servlet 5.0 version with new namespace.
 * Request to the simple snoop servlet 50 version which test several jakarta.servlet API (along with the new web-app web.xml namespace)
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
public class WC5JakartaServletTest {

    private static final Logger LOG = Logger.getLogger(WC5JakartaServletTest.class.getName());
    private static final String APP_NAME = "Servlet5TestApp";

    @Server("servlet50_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add " + APP_NAME + " war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "servlet5snoop.war.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WC5JakartaServletTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Request a simple snoop servlet.
     *
     * @throws Exception
     */
    @Test
    public void testSimple_Servlet50() throws Exception {
        String expectedResponse = "END OF SNOOP 5. TEST PASS";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/snoop5";

        LOG.info("url: " + url);
        LOG.info("expectedResponse: " + expectedResponse);

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

    /**
     * 
     * Ensure query param with no equals is registered as an empty string for servlet 5.0.
     *
     * @throws Exception
     */
    @Test
    public void testNoEqualsQueryParameter() throws Exception {
        String expectedResponse = "SUCCESS!";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/query?test";

        LOG.info("url: " + url);
        LOG.info("expectedResponse: " + expectedResponse);

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
