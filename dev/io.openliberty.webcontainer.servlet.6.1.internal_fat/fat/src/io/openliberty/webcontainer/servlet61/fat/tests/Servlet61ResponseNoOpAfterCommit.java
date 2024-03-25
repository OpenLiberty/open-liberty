/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.After;
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
 * Tests to ensure that specific methods in the ServletResponse and HttpServletResponse perform
 * no action once a response has been committed.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61ResponseNoOpAfterCommit {

    private static final Logger LOG = Logger.getLogger(Servlet61ResponseNoOpAfterCommit.class.getName());
    private static final String TEST_APP_NAME = "ResponseNoOpAfterCommit";
    private static String URL;

    @Server("servlet61_ResponseNoOpAfterCommit")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_ResponseNoOpAfterCommit test.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "response.noop.servlets");
        server.startServer(Servlet61ResponseNoOpAfterCommit.class.getSimpleName() + ".log");
        URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestResponseNoOpAfterCommit";
        LOG.info("Setup : startServer, ready for Tests.");
    }

    /**
     * After each test execution we restart the application so the SRVE8094W warning is output again.
     *
     * For performance reasons this is only done one time for the Servlet rather than filling up the logs
     * for each bad action.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        server.restartApplication(TEST_APP_NAME);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        // W SRVE8094W: WARNING: Cannot set header. Response already committed.
        // W SRVE8115W: WARNING: Cannot set status. Response already committed.
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8094W", "SRVE8115W");
        }
    }

    /**
     * Test that the ServletResponse.setContentLength method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_ServletRespose_setContentLength() throws Exception {
        String expectedResponse = "Hello from testServletResponse_setContentLength!";

        String responseText = performRequest("ServletResponse_setContentLength");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the ServletResponse.setContentLengthLong method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_ServletResponse_setContentLengthLong() throws Exception {
        String expectedResponse = "Hello from testServletResponse_setContentLengthLong!";

        String responseText = performRequest("ServletResponse_setContentLengthLong");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the HttpServletResponse.addCookie method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_addCookie() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_addCookie!";

        String responseText = performRequest("HttpServletResponse_addCookie");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the HttpServletResponse.setHeader method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_setHeader() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_setHeader!";

        String responseText = performRequest("HttpServletResponse_setHeader");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the HttpServletResponse.setIntHeader method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_setIntHeader() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_setIntHeader!";

        String responseText = performRequest("HttpServletResponse_setIntHeader");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the HttpServletResponse.addIntHeader method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_addIntHeader() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_addIntHeader!";

        String responseText = performRequest("HttpServletResponse_addIntHeader");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the HttpServletResponse.setStatus method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_setStatus() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_setStatus!";

        String responseText = performRequest("HttpServletResponse_setStatus");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8115W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8115W", server.getDefaultLogFile()).size() == 1);

    }

    /**
     * Test that the HttpServletResponse.setDateHeader method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_setDateHeader() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_setDateHeader!";

        String responseText = performRequest("HttpServletResponse_setDateHeader");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /**
     * Test that the HttpServletResponse.addDateHeader method is a no-op once a response has been committed.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletResponse_addDateHeader() throws Exception {
        String expectedResponse = "Hello from testHttpServletResponse_addDateHeader!";

        String responseText = performRequest("HttpServletResponse_addDateHeader");

        assertTrue("The response: " + responseText + " did not contain the expected response: " + expectedResponse, responseText.contains(expectedResponse));
        assertTrue("The exected SRVE8094W message was not found in the logs.", server.findStringsInLogsUsingMark("SRVE8094W", server.getDefaultLogFile()).size() == 1);
    }

    /*
     * Drive a request to the application and set the runTest header to the value of runTestHeaderValue so
     * the Servlet performs the correct set of actions.
     */
    private String performRequest(String runTestHeaderValue) throws Exception {
        String responseText;
        HttpGet getMethod = new HttpGet(URL);

        server.setMarkToEndOfLog();

        getMethod.addHeader("runTest", runTestHeaderValue);
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");
                Header[] headers = response.getHeaders();
                for (Header header:headers) {
                    LOG.info("\n" + "header: " + header.toString());
                }
            }
        }
        return responseText;
    }
}
