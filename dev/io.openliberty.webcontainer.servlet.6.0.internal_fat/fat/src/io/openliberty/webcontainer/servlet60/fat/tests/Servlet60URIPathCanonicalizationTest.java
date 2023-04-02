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
 * Part 1 - test the good cases.
 *
 * Example of URIs are from:
 * https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0.html#example-uris
 *
 * Note this is the pre-process URI path which is prior to map/match to a resource target (i.e servlet/filter/security).
 * There is not a way to test this pre-process steps via any request URI API;
 * Also need a valid ContextPath and ServletPath to generate result;
 *
 * so tests use the getPathInfo to verify the processed uri. i.e all these cases should be in pathInfo
 *
 * Tests here are for valid cases (i.e no bad request format). Bad requests are separated into another test.
 *
 * 1 /foo/bar ---> /foo/bar
 * 2 /foo/bar/ ---> /foo/bar/
 * 3 /foo/b%25r ---> /foo/b%r
 * 4 /foo/./bar ---> /foo/bar
 * 5 /foo/././bar ---> /foo/bar
 * 6 /./foo/bar ---> /foo/bar
 * 7 /foo/bar/. ---> /foo/bar
 * 8 /foo/bar/./ ---> /foo/bar/
 * 9 /foo/.bar ---> /foo/.bar
 * 10 /foo/../bar ---> /bar
 * 11 /foo/./../bar ---> /bar
 * 12 /foo/bar/.. ---> /foo
 * 13 /foo/bar/../ ---> /foo/
 * 14 /foo/..bar ---> /foo/..bar
 * 15 /foo/.../bar ---> /foo/.../bar
 * 16 /foo//bar ---> /foo/bar
 * 17 //foo//bar// ---> /foo/bar/
 * 18 /foo//../bar ---> /bar
 * 19 /foo%20bar ---> /foo bar
 * 20 /foo/bar?q ---> /foo/bar
 * 21 /foo/bar/?q ---> /foo/bar/
 * 22 /foo/bar;?q ---> /foo/bar
 * 23 / ---> /
 * 24 // ---> /
 * 25 /. ---> / >>>>> Correction: this should become empty/null (not /). See case #7
 * 26 /./ ---> /
 * 27 /?q ---> /
 *
 * //path parameters.
 * 28 /foo;/bar; ---> /foo/bar
 * 29 /foo;/bar;/; ---> /foo/bar/
 * 30 /foo/bar/./; ---> /foo/bar/
 * 31 /foo/bar/../; ---> /foo/
 *
 * //Special..skip this test as the server shows good result but client can't display it correctly.
 * 32 /foo%E2%82%ACbar ---> /foo€bar
 *
 * //path parameter with jsessonid
 * 33 /foo/bar;jsessionid=1234 ---> /foo/bar
 * 34 /foo/bar/;jsessionid=1234 ---> /foo/bar/
 *
 * Extra_1 /foo/./././././bar/. -> /foo/bar
 * Extra_2 /foo/././././..bar/. -> /foo/..bar
 * Extra_3 /foo///bar -> /foo/..bar //odd number of /
 *
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60URIPathCanonicalizationTest {
    private static final Logger LOG = Logger.getLogger(Servlet60URIPathCanonicalizationTest.class.getName());
    private static final String TEST_APP_NAME = "URIPathCanonicalizationTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet60_uriPathCanonicalizationTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, WAR_NAME, "uripath.servlets");

        server.startServer(Servlet60URIPathCanonicalizationTest.class.getSimpleName() + ".log");
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

    //1 /foo/bar ---> /foo/bar
    @Test
    public void test_URI_1() throws Exception {
        testRequest("/foo/bar", "pathInfo [/foo/bar]", "test_URI_1");
    }

    //2 /foo/bar/ ---> /foo/bar/
    @Test
    public void test_URI_2() throws Exception {
        testRequest("/foo/bar/", "pathInfo [/foo/bar/]", "test_URI_2");
    }

    //3 /foo/b%25r ---> /foo/b%r
    @Test
    public void test_URI_3() throws Exception {
        testRequest("/foo/b%25r", "pathInfo [/foo/b%r]", "test_URI_3");
    }

    //4 /foo/./bar ---> /foo/bar
    @Test
    public void test_URI_4() throws Exception {
        testRequest("/foo/./bar", "pathInfo [/foo/bar]", "test_URI_4");
    }

    //5 /foo/././bar ---> /foo/bar
    @Test
    public void test_URI_5() throws Exception {
        testRequest("/foo/././bar", "pathInfo [/foo/bar]", "test_URI_5");
    }

    //6 /./foo/bar ---> /foo/bar
    @Test
    public void test_URI_6() throws Exception {
        testRequest("/./foo/bar", "pathInfo [/foo/bar]", "test_URI_6");
    }

    //7 /foo/bar/. ---> /foo/bar
    @Test
    public void test_URI_7() throws Exception {
        testRequest("/foo/bar/.", "pathInfo [/foo/bar]", "test_URI_7");
    }

    //8 /foo/bar/./ ---> /foo/bar/
    @Test
    public void test_URI_8() throws Exception {
        testRequest("/foo/bar/./", "pathInfo [/foo/bar/]", "test_URI_8");
    }

    //9 /foo/.bar ---> /foo/.bar
    @Test
    public void test_URI_9() throws Exception {
        testRequest("/foo/.bar", "pathInfo [/foo/.bar]", "test_URI_9");
    }

    //10 /foo/../bar ---> /bar
    @Test
    public void test_URI_10() throws Exception {
        testRequest("/foo/../bar", "pathInfo [/bar]", "test_URI_10");
    }

    //11 /foo/./../bar ---> /bar
    @Test
    public void test_URI_11() throws Exception {
        testRequest("/foo/./../bar", "pathInfo [/bar]", "test_URI_11");
    }

    //12 /foo/bar/.. ---> /foo
    @Test
    public void test_URI_12() throws Exception {
        testRequest("/foo/bar/..", "pathInfo [/foo]", "test_URI_12");
    }

    //13 /foo/bar/../ ---> /foo/
    @Test
    public void test_URI_13() throws Exception {
        testRequest("/foo/bar/../", "pathInfo [/foo/]", "test_URI_13");
    }

    //14 /foo/..bar ---> /foo/..bar
    @Test
    public void test_URI_14() throws Exception {
        testRequest("/foo/..bar", "pathInfo [/foo/..bar]", "test_URI_14");
    }

    //15 /foo/.../bar ---> /foo/.../bar
    @Test
    public void test_URI_15() throws Exception {
        testRequest("/foo/.../bar", "pathInfo [/foo/.../bar]", "test_URI_15");
    }

    //16 /foo//bar ---> /foo/bar
    @Test
    public void test_URI_16() throws Exception {
        testRequest("/foo//bar", "pathInfo [/foo/bar]", "test_URI_16");
    }

    //17 //foo//bar// ---> /foo/bar/
    @Test
    public void test_URI_17() throws Exception {
        testRequest("//foo//bar//", "pathInfo [/foo/bar/]", "test_URI_17");
    }

    //18 /foo//../bar ---> /bar
    @Test
    public void test_URI_18() throws Exception {
        testRequest("/foo//../bar", "pathInfo [/bar]", "test_URI_18");
    }

    //19 /foo%20bar ---> /foo bar    //NOTE there is a space b/w foo and bar
    @Test
    public void test_URI_19() throws Exception {
        testRequest("/foo%20bar", "pathInfo [/foo bar]", "test_URI_19");
    }

    //20 /foo/bar?q ---> /foo/bar
    @Test
    public void test_URI_20() throws Exception {
        testRequest("/foo/bar?q", "pathInfo [/foo/bar]", "test_URI_20");
    }

    //21 /foo/bar/?q ---> /foo/bar/
    @Test
    public void test_URI_21() throws Exception {
        testRequest("/foo/bar/?q", "pathInfo [/foo/bar/]", "test_URI_21");
    }

    //22 /foo/bar;?q ---> /foo/bar
    @Test
    public void test_URI_22() throws Exception {
        testRequest("/foo/bar;?q", "pathInfo [/foo/bar]", "test_URI_22");
    }

    //23 / /
    @Test
    public void test_URI_23() throws Exception {
        testRequest("/", "pathInfo [/]", "test_URI_23");
    }

    //24 // /
    @Test
    public void test_URI_24() throws Exception {
        testRequest("//", "pathInfo [/]", "test_URI_24");
    }

    //25 /. /   /// CORRECTION: this should become empty/null
    @Test
    public void test_URI_25() throws Exception {
        testRequest("/.", "pathInfo [null]", "test_URI_25");
    }

    //26 /./ /
    @Test
    public void test_URI_26() throws Exception {
        testRequest("/./", "pathInfo [/]", "test_URI_26");
    }

    //27 /?q /
    @Test
    public void test_URI_27() throws Exception {
        testRequest("/?q", "pathInfo [/]", "test_URI_27");
    }

    //28 /foo;/bar; ---> /foo/bar
    @Test
    public void test_URI_28() throws Exception {
        testRequest("/foo;/bar;", "pathInfo [/foo/bar]", "test_URI_28");
    }

    //29 /foo;/bar;/; ---> /foo/bar/
    @Test
    public void test_URI_29() throws Exception {
        testRequest("/foo;/bar;/;", "pathInfo [/foo/bar/]", "test_URI_29");
    }

    //30 /foo/bar/./; ---> /foo/bar/
    @Test
    public void test_URI_30() throws Exception {
        testRequest("/foo/bar/./;", "pathInfo [/foo/bar/]", "test_URI_30");
    }

    //31 /foo/bar/../; ---> /foo/
    @Test
    public void test_URI_31() throws Exception {
        testRequest("/foo/bar/../;", "pathInfo [/foo/]", "test_URI_31");
    }

    //32 /foo%E2%82%ACbar ---> /foo€bar
    /*
     * The server log show the correct value pathInfo [/foo€bar]
     * [10/31/22, 21:39:08:551 EDT] 0000004f BNFHeadersImp 1 Adding header [TestResult] with value [ Message - pathInfo [/foo€bar] , servletPath [/DecodeNormalizeURI] , contextPath
     * [/URIPathCanonicalizationTest] , reqURL [http://localhost:8010/URIPathCanonicalizationTest/DecodeNormalizeURI/foo%E2%82%ACbar] , reqURI
     * [/URIPathCanonicalizationTest/DecodeNormalizeURI/foo%E2%82%ACbar]]
     *
     * BUT Client output.txt show pathInfo [/foo¬bar]
     * [10/31/2022 21:39:08:599 EDT] 001 Servlet60URIPathCanonicalizati testRequest I
     * TestResult : Message - pathInfo [/foo¬bar] , servletPath [/DecodeNormalizeURI] , contextPath [/URIPathCanonicalizationTest] , reqURL
     * [http://localhost:8010/URIPathCanonicalizationTest/DecodeNormalizeURI/foo%E2%82%ACbar] , reqURI [/URIPathCanonicalizationTest/DecodeNormalizeURI/foo%E2%82%ACbar]
     *
     * Comment out this test
     *
     * @Test
     * public void test_URI_32() throws Exception {
     * testRequest("/foo%E2%82%ACbar", "pathInfo [/foo€bar]", "test_URI_32");
     * }
     */

    //33 /foo/bar;jsessionid=1234 ---> /foo/bar
    @Test
    public void test_URI_33() throws Exception {
        testRequest("/foo/bar;jsessionid=1234", "pathInfo [/foo/bar]", "test_URI_33");
    }

    //34 /foo/bar/;jsessionid=1234 ---> /foo/bar/
    @Test
    public void test_URI_34() throws Exception {
        testRequest("/foo/bar/;jsessionid=1234", "pathInfo [/foo/bar/]", "test_URI_34");
    }

    //Extra_1 /foo/./././././bar/.   -> /foo/bar
    @Test
    public void test_URI_Extra_1() throws Exception {
        testRequest("/foo/./././././bar/.", "pathInfo [/foo/bar]", "test_URI_Extra_1");
    }

    //Extra_2 /foo/././././..bar/.   -> /foo/..bar
    @Test
    public void test_URI_Extra_2() throws Exception {
        testRequest("/foo/././././..bar/.", "pathInfo [/foo/..bar]", "test_URI_Extra_2");
    }

    //Extra_3 /foo///bar   -> /foo/..bar          //odd number of /
    @Test
    public void test_URI_Extra_3() throws Exception {
        testRequest("/foo///bar", "pathInfo [/foo/bar]", "test_URI_Extra_3");
    }

    //4 /../jsp/../landPage.jsp  //JSP good format.  Bad case will have similar tests but in bad form
    @Test
    public void test_URI_Extra_4() throws Exception {
        testRequest("/../jsp/../landingPage.jsp", "Dummy Test JSP", "test_URI_Extra_4");
    }
}
