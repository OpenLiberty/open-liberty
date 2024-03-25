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
import org.apache.hc.client5.http.classic.methods.HttpPost;
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
 * Test request error attribute jakarta.servlet.error.method and request.getMethod() throughout the request-error cycle
 *
 * Initial Request method is [POST] --- Dispatching-to-Error-Page ---> During error page dispatch method is [GET]
 *                                   --- return-from-Error-Dispatch --> After exited error page dispatch, method is restored to [POST]
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61DispatcherErrorMethodAttributeTest {

    private static final Logger LOG = Logger.getLogger(Servlet61DispatcherErrorMethodAttributeTest.class.getName());
    private static final String TEST_APP_NAME = "DispatcherErrorMethodAttributeTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet61_DispatcherErrorMethodTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_DispatcherErrorMethodTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "errormethod.servlets");
        server.startServer(Servlet61DispatcherErrorMethodAttributeTest.class.getSimpleName() + ".log");
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

    /*
     * Test request attribute "jakarta.servlet.error.method"
     */
    @Test
    public void test_Dispatcher_requestErrorMethodAttribute() throws Exception {
        LOG.info("====== <test_Dispatcher_requestErrorMethodAttribute> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestErrorMethodAttribute";
        HttpPost httpMethod = new HttpPost(url);         //POST without any body
        httpMethod.addHeader("runTest", "test_ErrorMethod_Attribute");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("FOUND a FAIL result. Check log" , !responseText.contains("FAIL"));
            }
        }
    }

    /*
     * Test request attribute RequestDispacher.ERROR_QUERY_STRING "jakarta.servlet.error.query_string" retrieved from inside the error page
     */
    @Test
    public void test_Dispatcher_requestErrorQueryStringAttribute() throws Exception {
        LOG.info("====== <test_Dispatcher_requestErrorQueryStringAttribute> ======");

        final String QUERYSTRING = "QueryString=TestValue&QS2=Value2&TestQS=Value3";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestErrorMethodAttribute" + "?" + QUERYSTRING;
        HttpGet httpMethod = new HttpGet(url);

        httpMethod.addHeader("runTest", "test_ErrorQueryString_Attribute");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Expected response contains string [" + QUERYSTRING + "] but found [" + responseText + "]", responseText.trim().contains(QUERYSTRING));
            }
        }
    }
}
