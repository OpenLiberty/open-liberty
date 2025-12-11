/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet60.fat.tests;

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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the server Directory Browsing function using servletContext.getRealPath
 * instead of the removed request.getRealPath.
 *
 * Request to the context root. The response should contain a list of some folders
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60GetRealPathDirectoryBrowsingTest {
    private static final Logger LOG = Logger.getLogger(Servlet60GetRealPathDirectoryBrowsingTest.class.getName());
    private static final String APP_NAME = "GetRealPathDirectoryBrowsing";

    @Server("servlet60_getRealPathDirectoryBrowsing")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup: servlet60_getRealPathDirectoryBrowsing");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
        server.startServer(Servlet60GetRealPathDirectoryBrowsingTest.class.getSimpleName() + ".log");

        LOG.info("Setup : startServer, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp: stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Request to the context root.
     * Expect the response contains "FirstFolder" and "SecondFolder" which are folders at the root
     *
     * @throws Exception
     */
    @Test
    public void test_directoryBrowsing() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/";
        String expectedResp1 = "FirstFolder";
        String expectedResp2 = "SecondFolder";

        LOG.info("Sending request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following string: " + expectedResp1, responseText.contains(expectedResp1));
                assertTrue("The response did not contain the following string: " + expectedResp2, responseText.contains(expectedResp2));
            }
        }
    }
}
