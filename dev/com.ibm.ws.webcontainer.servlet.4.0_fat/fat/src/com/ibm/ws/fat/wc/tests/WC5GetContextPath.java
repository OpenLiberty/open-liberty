/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 * Test ServletContext.getContextPath() and HttpServletRequest.getContextPath().
 * The application is purposely configured with an illegal ending slash (/) via the server.xml's webApplication element
 * The above APIs should return WITHOUT the ending slash.
 * The target JSP file will do all the query, verify, and response with PASS or FAIL for each API.
 * Should only run in EE9.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
public class WC5GetContextPath {

    private static final Logger LOG = Logger.getLogger(WC5GetContextPath.class.getName());
    private static final String APP_NAME = "Servlet5TestApp";

    @Server("servlet50_GetContextPath")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add " + APP_NAME + " war to the server's apps folder if not already present.");

        ShrinkHelper.defaultApp(server, APP_NAME + ".war", "servlet5snoop.war.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WC5GetContextPath.class.getSimpleName() + ".log");
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
     * Request to testGetContextPath.jsp
     *
     * @throws Exception
     */
    @Test
    public void test_Servlet5_GetContextPath() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + "TestGetContextPathRoot" + "/testGetContextPath.jsp";

        LOG.info("url: " + url);
        LOG.info("expectedResponse: Verify that no FAIL in the response text");

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: ",
                           responseText.contains("request.getContextPath()=[PASS]") && responseText.contains("servletContext.getContextPath()=[PASS]"));
            }
        }
    }
}