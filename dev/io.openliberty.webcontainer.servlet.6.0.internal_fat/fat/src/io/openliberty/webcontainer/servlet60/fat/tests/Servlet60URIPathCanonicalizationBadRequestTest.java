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
 * Test the bad request URI. (part 2 of URI Path Canonicalization)
 *
 * All tests generate 400 Bad Request response. Since there can be various reasons for 400,
 * tests just verify the 400 status.
 *
 * Some tests response with 404.
 *
 * Example of URIs from:
 * https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0.html#example-uris
 *
 * 1 /#f /
 * 2 /foo/bar#f /foo/bar
 * 3 /foo%2Fbar /foo%2Fbar
 * 4 /foo%2Fb%25r /foo%2Fb%25r
 * 5 /foo\\bar /foo\\bar
 * 6 /foo%5Cbar /foo\\bar
 * 7 /foo;%2F/bar /foo/bar
 * 8 /foo/%2e/bar /foo/bar
 * 9 /foo/.;/bar /foo/bar
 * 10 /foo/%2e;/bar /foo/bar
 * 11 /foo/.%2Fbar /foo/.%2Fbar
 * 12 /foo/.%5Cbar /foo/.\\bar
 * 13 /foo/bar/.; /foo/bar
 * 14 /foo/../../bar /../bar
 * 15 /../foo/bar /../foo/bar
 * 16 /foo/%2e%2E/bar /bar
 * 17 /foo/%2e%2e/%2E%2E/bar /../bar
 * 18 /foo/..;/bar /bar
 * 19 /foo/%2e%2E;/bar /bar
 * 20 /foo/..%2Fbar /foo/..%2Fbar
 * 21 /foo/..%5Cbar /foo/..\\bar
 * 22 /foo/bar/..; /foo
 * 23 /;/foo;/;/bar/;/; /foo/bar/
 * 24 /foo/;/../bar /bar
 * 25 /foo/bar/#f /foo/bar/
 * 26 /foo/bar;#f /foo/bar
 * 27 /;/ /
 * 28 /.. /..
 * 29 /../ /../
 * 30 /;/foo/bar/ /foo/bar/
 * 31 /foo%00/bar/ /foo[NUL]/bar/ control character
 * 32 /foo%7Fbar /foo[DEL]bar control character
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60URIPathCanonicalizationBadRequestTest {
    private static final Logger LOG = Logger.getLogger(Servlet60URIPathCanonicalizationBadRequestTest.class.getName());
    private static final String TEST_APP_NAME = "URIPathCanonicalizationTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet60_uriPathCanonicalizationTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, WAR_NAME, "uripath.servlets");

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("SRVE0190E:.*"); //File not found 404
        expectedErrors.add("CWWWC0005I:.*"); //400 Bad Request
        server.addIgnoredErrors(expectedErrors);

        server.startServer(Servlet60URIPathCanonicalizationBadRequestTest.class.getSimpleName() + ".log");
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

    //1 /#f /  ; fragment
    // client or network drop the # before getting into WC, so use its encoded %23 instead
    @Test
    public void test_BadURI_1() throws Exception {
        testRequest("/%23f", 400, "test_BadURI_1");
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

    //4 /foo%2Fb%25r /foo%2Fb%25r ; encoded /
    @Test
    public void test_BadURI_4() throws Exception {
        testRequest("/foo%2Fb%25r", 400, "test_BadURI_4");
    }

    /*
     * //5 /foo\\bar /foo\\bar ; backslash character
     * Httpclient rejects this request: Illegal character in path at index 72: http://localhost:8010/URIPathCanonicalizationTest/DecodeNormalizeURI/foo\bar
     * use encoded %5c instead
     */
    @Test
    public void test_BadURI_5() throws Exception {
        testRequest("/foo%5c%5cbar", 400, "test_BadURI_5");
    }

    //6 /foo%5Cbar /foo\bar    ; encoded backslash
    @Test
    public void test_BadURI_6() throws Exception {
        testRequest("/foo%5Cbar", 400, "test_BadURI_6");
    }

    //7 /foo;%2F/bar /foo/bar  ; encoded /
    @Test
    public void test_BadURI_7() throws Exception {
        testRequest("/foo;%2F/bar", 400, "test_BadURI_7");
    }

    //8 /foo/%2e/bar /foo/bar  ; encoded dot segment
    @Test
    public void test_BadURI_8() throws Exception {
        testRequest("/foo/%2e/bar", 400, "test_BadURI_8");
    }

    //9 /foo/.;/bar /foo/bar   ; dot segment with parameter
    @Test
    public void test_BadURI_9() throws Exception {
        testRequest("/foo/.;/bar", 400, "test_BadURI_9");
    }

    //10 /foo/%2e;/bar /foo/bar ; encoded dot segment
    @Test
    public void test_BadURI_10() throws Exception {
        testRequest("/foo/%2e;/bar", 400, "test_BadURI_10");
    }

    //11 /foo/.%2Fbar /foo/.%2Fbar ;     encoded /
    @Test
    public void test_BadURI_11() throws Exception {
        testRequest("/foo/.%2Fbar", 400, "test_BadURI_11");
    }

    //12 /foo/.%5Cbar /foo/.\\bar      ; backslash character
    @Test
    public void test_BadURI_12() throws Exception {
        testRequest("/foo/.%5Cbar", 400, "test_BadURI_12");
    }

    //13 /foo/bar/.; /foo/bar  ; dot segment with parameter
    @Test
    public void test_BadURI_13() throws Exception {
        testRequest("/foo/bar/.;", 400, "test_BadURI_13");
    }

    /*
     * //14 /foo/../../bar /../bar ; leading dot-dot-segment
     *
     * The actual URI is /URIPathCanonicalizationTest/DecodeNormalizeURI/foo/../../bar
     * which will canonicalize to /URIPathCanonicalizationTest/bar and results in 404
     */

    @Test
    public void test_BadURI_14() throws Exception {
        testRequest("/foo/../../bar", 404, "test_BadURI_14");
    }

    /*
     * //15 /../foo/bar /../foo/bar ; leading dot-dot-segment
     *
     * The actual URI is /URIPathCanonicalizationTest/DecodeNormalizeURI/../foo/bar
     * which will canonicalize to /URIPathCanonicalizationTest/foo/bar and results in 404
     */

    @Test
    public void test_BadURI_15() throws Exception {
        testRequest("/../foo/bar", 404, "test_BadURI_15");
    }

    //16 /foo/%2e%2E/bar /bar  ; encoded dot segment
    @Test
    public void test_BadURI_16() throws Exception {
        testRequest("/foo/%2e%2E/bar", 400, "test_BadURI_16");
    }

    //17 /foo/%2e%2e/%2E%2E/bar /../bar        ; leading dot-dot-segment & encoded dot segment
    // this one also test the DefaultExtensionProcessor path which verifies and rejected encoded .
    @Test
    public void test_BadURI_17() throws Exception {
        testRequest("/foo/%2e%2e/%2E%2E/bar", 400, "test_BadURI_17");
    }

    //18 /foo/..;/bar /bar       ; dot segment with parameter
    @Test
    public void test_BadURI_18() throws Exception {
        testRequest("/foo/..;/bar", 400, "test_BadURI_18");
    }

    //19 /foo/%2e%2E;/bar /bar  ; encoded dot segment
    @Test
    public void test_BadURI_19() throws Exception {
        testRequest("/foo/%2e%2E;/bar", 400, "test_BadURI_19");
    }

    //20 /foo/..%2Fbar /foo/..%2Fbar    ; encoded /
    @Test
    public void test_BadURI_20() throws Exception {
        testRequest("/foo/..%2Fbar", 400, "test_BadURI_20");
    }

    //21 /foo/..%5Cbar /foo/..\\bar     ; backslash character
    @Test
    public void test_BadURI_21() throws Exception {
        testRequest("/foo/..%5Cbar", 400, "test_BadURI_21");
    }

    //22 /foo/bar/..; /foo      ; dot segment with parameter
    @Test
    public void test_BadURI_22() throws Exception {
        testRequest("/foo/bar/..;", 400, "test_BadURI_22");
    }

    //23 /;/foo;/;/bar/;/; /foo/bar/    ; empty segment with parameters
    @Test
    public void test_BadURI_23() throws Exception {
        testRequest("/;/foo;/;/bar/;/;", 400, "test_BadURI_23");
    }

    //24 /foo/;/../bar /bar     ; empty segment with parameters
    @Test
    public void test_BadURI_24() throws Exception {
        testRequest("/foo/;/../bar", 400, "test_BadURI_24");
    }

    //25 /foo/bar/#f /foo/bar/  ; fragment
    // use encode %23 for #
    @Test
    public void test_BadURI_25() throws Exception {
        testRequest("/foo/bar/%23f", 400, "test_BadURI_25");
    }

    //26 /foo/bar;#f /foo/bar   ; fragment
    // use encode %23 for #
    @Test
    public void test_BadURI_26() throws Exception {
        testRequest("/foo/bar;%23f", 400, "test_BadURI_26");
    }

    //27 /;/ /  ; empty segment with parameters
    @Test
    public void test_BadURI_27() throws Exception {
        testRequest("/;/", 400, "test_BadURI_27");
    }

    /*
     * 28 /.. /.. ; leading dot-dot-segment
     * /URIPathCanonicalizationTest/DecodeNormalizeURI/.. becomes /URIPathCanonicalizationTest
     * which results in 404
     */
    @Test
    public void test_BadURI_28() throws Exception {
        testRequest("/..", 404, "test_BadURI_28");
    }

    /*
     * //29 /../ /../ ; leading dot-dot-segment
     * /URIPathCanonicalizationTest/DecodeNormalizeURI/../ becomes /URIPathCanonicalizationTest/
     * results in 404
     */
    @Test
    public void test_BadURI_29() throws Exception {
        testRequest("/../", 404, "test_BadURI_29");
    }

    //30 /;/foo/bar/ /foo/bar/   ; empty segment with parameters
    @Test
    public void test_BadURI_30() throws Exception {
        testRequest("/;/foo/bar/", 400, "test_BadURI_30");
    }

    //31 /foo/%00/bar       //Control %00 is NULL character
    @Test
    public void test_BadURI_31() throws Exception {
        testRequest("/foo%00/bar", 400, "test_BadURI_31");
    }

    //32 /foo%7F        control character DEL
    @Test
    public void test_BadURI_32() throws Exception {
        testRequest("/foo%7F/bar", 400, "test_BadURI_32");
    }

    //33 /../jsp/%2E%2e/landingPage.jsp   /../jsp to escape the servlet mapping in order to trigger JSP path with %2E;  also errorPage path
    @Test
    public void test_BadURI_33() throws Exception {
        testRequest("/../jsp/%2E%2e/landingPage.jsp", 400, "test_BadURI_33");
    }

    //34 /../jsp/..%2FlandingPage.jsp      //test JSP path with encoded forwardslash %2f ;  also errorPage path
    @Test
    public void test_BadURI_34() throws Exception {
        testRequest("/../jsp/..%2flandingPage.jsp", 400, "test_BadURI_34");
    }
}
