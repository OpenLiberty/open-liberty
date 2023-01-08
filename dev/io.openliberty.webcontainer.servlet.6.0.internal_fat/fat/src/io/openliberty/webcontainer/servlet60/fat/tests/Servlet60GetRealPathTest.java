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
 * Test to ensure that the Servlet implementation abides by the following:
 *
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletcontext#getRealPath(java.lang.String)
 *
 * "The path should begin with a / and is interpreted as relative to the current context root. If the path does not begin with a /,
 * the container will behave as if the method was called with / appended to the beginning of the provided path. "
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60GetRealPathTest {

    private static final Logger LOG = Logger.getLogger(Servlet60GetRealPathTest.class.getName());
    private static final String GET_REAL_PATH_TEST_APP_NAME = "GetRealPathTest";

    @Server("servlet60_getRealPathTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, GET_REAL_PATH_TEST_APP_NAME + ".war", "getrealpath.servlets");

        server.startServer(Servlet60GetRealPathTest.class.getSimpleName() + ".log");
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
     * Test to ensure that when calling ServletContext.getRealPath(String path) with and without "/" that
     * the same real path is returned.
     *
     * @throws Exception
     */
    @Test
    public void test_getRealPathClarification() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + GET_REAL_PATH_TEST_APP_NAME + "/GetRealPathServlet";
        String expectedResponse = "ServletContext getRealPath returned the same value with and without a forward slash. Test PASSED!";

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
