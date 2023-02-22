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
import static org.junit.Assert.fail;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test server setting to skip the encoded character verification in the request URI for a servlet
 * <webContainer skipEncodedCharVerification="true"/>
 *
 * %2E (dot), %2F(forward-slash), %23 (fragment # encoded or not) and %5C (back-slash encoded or not) in the request URI
 * should NOT be rejected.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60URIPathCanonicalizationServerXMLTest {
    private static final Logger LOG = Logger.getLogger(Servlet60URIPathCanonicalizationServerXMLTest.class.getName());
    private static final String TEST_APP_NAME = "URIPathCanonicalizationTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet60_uriPathCanonicalServerXMLTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, WAR_NAME, "uripath.servlets");

        server.startServer(Servlet60URIPathCanonicalizationServerXMLTest.class.getSimpleName() + ".log");
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
     * Prelude test to make sure app is working
     *
     * contextPath = URIPathCanonicalizationTest
     * servletPath = DecodeNormalizeURI
     * pathInfo = null
     */
    @Test
    public void test_URI_0() throws Exception {
        String expected = "pathInfo [null] , servletPath [/DecodeNormalizeURI] , contextPath [/URIPathCanonicalizationTest]";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/DecodeNormalizeURI";

        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        //use header to send test name so that we have a clean URI
        getMethod.addHeader("TEST_NAME", "test_DecodeURI_0");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");
                String headerValue = response.getHeader("TestResult").getValue();
                LOG.info("\n TestResult : " + headerValue);

                assertTrue("The response does not contain <" + expected + "> . TestResult header [" + headerValue + "]", headerValue.contains(expected));
            }
        }
    }

    /**
     * expectedResult - Make sure the response is wrapped inside []
     */
    private void testRequest(String testPath, String expectedResult, String testMethod) throws Exception {
        LOG.info(testMethod + " [" + testPath + "] , expecting [" + expectedResult + "]");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME
                     + "/DecodeNormalizeURI" + testPath;

        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);
        getMethod.addHeader("TEST_NAME", testMethod + " , testing [" + testPath + "]"); //look for this in the server trace

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                Header h = response.getHeader("TestResult");

                if (h != null) {
                    String headerValue = h.getValue();
                    LOG.info("\n TestResult : " + headerValue);
                    assertTrue("The response does not contain header <" + expectedResult + "> . TestResult header [" + headerValue + "]", headerValue.contains(expectedResult));
                } else if (responseText != null) {
                    LOG.info("\n" + "Response Text: [" + responseText + "]");
                    assertTrue("The response does not contain <" + expectedResult + "> . ResponseText [" + responseText + "]", responseText.contains(expectedResult));
                } else {
                    fail("No TestResult header or ResponseText found");
                }
            }
        }
    }

    //1 encoded fragment %23 test
    @Test
    public void test_URI_1() throws Exception {
        testRequest("/fragment%23test", "pathInfo [/fragment#test]", "test_URI_1");
    }

    //2 encoded forward %2F test
    @Test
    public void test_URI_2() throws Exception {
        testRequest("/forward%2Ftest", "pathInfo [/forward/test]", "test_URI_2");
    }

    //3 encoded dot %2E
    @Test
    public void test_URI_3() throws Exception {
        testRequest("/dot%2Etest", "pathInfo [/dot.test]", "test_URI_3");
    }

    //4 encoded back-slash %5C  - expected string needs two \\ as the first one is escape char
    @Test
    public void test_URI_4() throws Exception {
        testRequest("/back-slash%5Ctest", "pathInfo [/back-slash\\test]", "test_URI_4");
    }
}
