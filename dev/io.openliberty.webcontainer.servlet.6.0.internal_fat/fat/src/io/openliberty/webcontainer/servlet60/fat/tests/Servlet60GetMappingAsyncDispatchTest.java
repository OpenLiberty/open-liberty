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
 * 1. Test the servlet mapping in async dispatch:
 *
 * DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR
 * Return the mapping for the target of the dispatch i.e. the mapping for the current Servlet.
 *
 * Target/dispatched servlet verifies that uri path, url-pattern, servletName, and mappingMatch are correct from the asyc dispatched servlet
 *
 * 2. Also test the request attribute at the target/dispatched servlet to make sure they reflect the info of the original/caller servlet
 *
 * Other async dispatch tests are covered in servlet.4.0_fat/WCGetMappingTest which is also updated to cover the change in Servlet 6.0 for this
 * area.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60GetMappingAsyncDispatchTest {
    private static final Logger LOG = Logger.getLogger(Servlet60GetMappingAsyncDispatchTest.class.getName());
    private static final String TEST_APP_NAME = "GetMappingAsyncDispatchTest";

    @Server("servlet60_getMappingAsyncDispatchTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "servlets");

        server.startServer(Servlet60GetMappingAsyncDispatchTest.class.getSimpleName() + ".log");
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
     * Test HttpServletMapping info in the async dispatched servlet which verifies and report the result in the
     * response header "TestResult"
     */
    @Test
    public void test_GetMappingAndAttributesAsyncDispatch() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/AsyncServlet";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                String headerValue = response.getHeader("TestResult").getValue();

                LOG.info("\n TestResult : " + headerValue);

                assertTrue("The response does not contain Result [PASS]. TestResult header [" + headerValue + "]", headerValue.contains("Result [PASS]"));
            }
        }
    }
}
