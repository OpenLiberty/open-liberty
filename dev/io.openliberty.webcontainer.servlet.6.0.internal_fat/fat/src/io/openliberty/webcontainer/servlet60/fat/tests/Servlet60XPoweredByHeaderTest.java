/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.webcontainer.servlet60.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to ensure that <webContainer disableXPoweredBy="false"/> is ignored in the
 * server configuration for Servlet 6.0+.
 */
@RunWith(FATRunner.class)
public class Servlet60XPoweredByHeaderTest {

    private static final Logger LOG = Logger.getLogger(Servlet60XPoweredByHeaderTest.class.getName());
    private static final String SIMPLE_TEST_APP_NAME = "SimpleTest";

    @Server("servlet60_xPoweredByHeaderTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, SIMPLE_TEST_APP_NAME + ".war", "simpletest.servlets");

        server.startServer(Servlet60XPoweredByHeaderTest.class.getSimpleName() + ".log");
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
     * Test to ensure that if <webContainer disableXPoweredBy="false"/> is configured in the
     * server.xml that it does not work for Servlet 6.0+.
     *
     * @throws Exception
     */
    @Test
    public void test_XPoweredByCanNotBeEnabled() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + SIMPLE_TEST_APP_NAME + "/TestServlet";
        String expectedResponse = "Hello from the TestServlet!!";

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                // Validate that the X-Powered-By header was not present in the response
                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    LOG.info("\n" + "Header: " + header);
                }

                Header header = response.getHeader("X-Powered-By");
                assertTrue("The X-Powered-By header was found when it should not have been.", header == null);
            }
        }
    }
}
