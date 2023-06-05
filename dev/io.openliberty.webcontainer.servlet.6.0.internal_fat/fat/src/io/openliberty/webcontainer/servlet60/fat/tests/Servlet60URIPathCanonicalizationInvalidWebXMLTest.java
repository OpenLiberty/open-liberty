/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet60.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
 * Test the invalid configure value from web.xml for property SKIP_ENCODED_CHAR_VERIFICATION
 *
 * Send few invalid encoded tests (%23, %2E, %2F, %5C) and verify 400 Bad Request response.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60URIPathCanonicalizationInvalidWebXMLTest {
    private static final Logger LOG = Logger.getLogger(Servlet60URIPathCanonicalizationInvalidWebXMLTest.class.getName());
    private static final String TEST_APP_NAME = "URIPathCanonicalInvalidWebXmlTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet60_uriPathCanonicalInvalidWebXMLTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, WAR_NAME, "uripathinvalidwebxml.servlets");

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0005I:.*"); //400 Bad Request
        server.addIgnoredErrors(expectedErrors);

        server.startServer(Servlet60URIPathCanonicalizationInvalidWebXMLTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private void testRequest(String testPath, int expectedStatus, String testMethod) throws Exception {
        LOG.info(testMethod + " [" + testPath + "]");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/DecodeNormalizeURI" + testPath;

        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);
        getMethod.addHeader("TEST_NAME", testMethod + " , testing [" + testPath + "]");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {

                int statusCode = response.getCode();
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response code: [" + statusCode + "] || Response Text: [" + responseText + "]");

                assertTrue("The expected status code [" + expectedStatus + "] . Actual status code [" + statusCode + "]", statusCode == expectedStatus);

                if (expectedStatus == 400)
                    assertTrue("The response code does not contain [CWWWC0005I]. Actual response [" + responseText + "]", responseText.contains("CWWWC0005I"));
            }
        }
    }

    //2 /foo/bar#f /foo/bar      ; fragment
    // client or network drop the # before getting into WC, so use its encoded %23 instead
    @Test
    public void test_BadURI_2() throws Exception {
        testRequest("/foo/bar%23f", 400, "test_BadURI_2");
    }

    //3 /foo%2Fbar /foo%2Fbar  ;  encoded /
    @Test
    public void test_BadURI_3() throws Exception {
        testRequest("/foo%2Fbar", 400, "test_BadURI_3");
    }

    //4 /foo%5Cbar /foo\bar    ; encoded backslash
    @Test
    public void test_BadURI_4() throws Exception {
        testRequest("/foo%5Cbar", 400, "test_BadURI_4");
    }

    //5 /foo/%2e/bar /foo/bar  ; encoded dot segment
    @Test
    public void test_BadURI_5() throws Exception {
        testRequest("/foo/%2e/bar", 400, "test_BadURI_5");
    }
}
