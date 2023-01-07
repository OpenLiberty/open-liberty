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
 * Test Response setCharacterEncoding(null), setContenType(null), setLocale(null)
 *
 * request URL: /TestResponseNullCharacterEncoding?testName=<select-methods>
 * Application servlet calls and verifies all the results. It sends back a response with header "TestResult" that contains all the data.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60ResponseNullCharacterEncodingTest {
    private static final Logger LOG = Logger.getLogger(Servlet60ResponseNullCharacterEncodingTest.class.getName());
    private static final String TEST_APP_NAME = "ResponseNullCharacterEncodingTest";

    @Server("servlet60_responseNullCharacterEncodingTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "characterencoding.servlets");

        server.startServer(Servlet60ResponseNullCharacterEncodingTest.class.getSimpleName() + ".log");
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
     * Test response.setCharacterEncoding("UTF-8") - Expect: UTF-8 encoding
     * response.setCharacterEncoding(null) - Expect: default (ISO-8859-1) encoding
     */
    @Test
    public void test_SetCharacterEncodingNull() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/TestResponseNullCharacterEncoding?testName=setCharacterEncoding";

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

    /*
     * 1st: Test response.setContentType("UTF-8") - Expect: UTF-8 encoding; then response.setCharacterEncoding(null) - Expect: default encoding
     * 2nd: Test response.setContentType("UTF-8") - Expect: UTF-8 encoding; then response.setContentType(null) - Expect: default encoding
     */
    @Test
    public void test_SetContentTypeNull() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/TestResponseNullCharacterEncoding?testName=setContentType";

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

    /*
     * 1st - Test response.setLocale("ja"), then setCharacterEncoding(null). - enc set back to default
     * 2nd - Test response.setLocale("ja"), then setLocale(null) - enc is set back to default
     * 3rd - Test response.setCharacterEncoding(UTF-8), then setLocale(null) - the enc remains UTF-8
     */
    @Test
    public void test_SetLocaleNull() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/TestResponseNullCharacterEncoding?testName=setLocale";

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

    /*
     * Test response.setCharacterEncoding("invalid-encoding"), verify "invalid-encoding" encoding but does not throw exception
     * Test response.getWriter() which now throws UnsupportedEncodingException
     */
    @Test
    public void test_SetInvalidEncoding() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/TestResponseNullCharacterEncoding?testName=invalidEncoding";

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
