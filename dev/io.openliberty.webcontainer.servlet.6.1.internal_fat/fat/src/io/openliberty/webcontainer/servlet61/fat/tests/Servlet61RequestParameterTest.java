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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
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
 * Test the functionality of the getParameter family to throw IllegalStateException
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61RequestParameterTest {
    private static final Logger LOG = Logger.getLogger(Servlet61RequestParameterTest.class.getName());
    private static final String TEST_APP_NAME = "RequestParameterTest";
    private static final String BAD_KEY_POST_DATA = "POST_KEY1=POST_VALUE_1&%XYZ_KEY=POST_HAS_BAD_KEY";
    private static final String BAD_VALUE_POST_DATA = "POST_KEY1=POST_VALUE_1&POST_HAD_BAD_VALUE=%XYZ_VALUE";

    @Server("servlet61_RequestParameterTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_RequestParameterTest.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "getparameter.servlets");
        server.startServer(Servlet61RequestParameterTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void test_Request_getParameter_badKey() throws Exception {
        LOG.info("====== <test_Request_getParameter_badKey> ======");
        runTest("testGetParameter", BAD_KEY_POST_DATA);
    }

    @Test
    public void test_Request_getParameter_badValue() throws Exception {
        LOG.info("====== <test_Request_getParameter_badValue> ======");
        runTest("testGetParameter", BAD_VALUE_POST_DATA);
    }

    @Test
    public void test_Request_getParameterNames_badKey() throws Exception {
        LOG.info("====== <test_Request_getParameterNames_badKey> ======");
        runTest("testGetParameterNames", BAD_KEY_POST_DATA);
    }

    @Test
    public void test_Request_getParameterNames_badValue() throws Exception {
        LOG.info("====== <test_Request_getParameterNames_badValue> ======");
        runTest("testGetParameterNames", BAD_VALUE_POST_DATA);
    }

    @Test
    public void test_Request_getParameterValues_badKey() throws Exception {
        LOG.info("====== <test_Request_getParameterValues_badKey> ======");
        runTest("testGetParameterValues", BAD_KEY_POST_DATA);
    }

    @Test
    public void test_Request_getParameterValues_badValue() throws Exception {
        LOG.info("====== <test_Request_getParameterValues_badValue> ======");
        runTest("testGetParameterValues", BAD_VALUE_POST_DATA);
    }

    @Test
    public void test_Request_getParameterMap_badKey() throws Exception {
        LOG.info("====== <test_Request_getParameterMap_badKey> ======");
        runTest("testGetParameterMap", BAD_KEY_POST_DATA);
    }

    @Test
    public void test_Request_getParameterMap_badValue() throws Exception {
        LOG.info("====== <test_Request_getParameterMap_badValue> ======");
        runTest("testGetParameterMap", BAD_VALUE_POST_DATA);
    }

    /**
     * Since all tests end with IllegalStateException in a response message, assertTrue on its existing.
     * @param testToRun - name of the test/method for the servlet to execute
     * @param postData - request POST form data
     */
    private void runTest(String testToRun, String postData) throws Exception {
        LOG.info("====== <runTest> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestGetParameter";
        HttpPost httpMethod = new HttpPost(url);

        httpMethod.addHeader("runTest", testToRun);

        //Need this header when use with StringEntity() to tell server treat it as form
        //dont use UrlEncodedFormEntity as it will mess up the % in the key/value
        httpMethod.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpMethod.setEntity(new StringEntity(postData));

        LOG.info("Sending [" + url + "]");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Expecting IllegalStateException in response but not found. Actual response [" + responseText + "]", responseText.contains("IllegalStateException"));
            }
        }
    }
}
