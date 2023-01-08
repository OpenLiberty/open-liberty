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
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class PrivateHeaderTest {
    private static final Logger LOG = Logger.getLogger(PrivateHeaderTest.class.getName());

    @Server("servlet31_privateHeaderServer")
    public static LibertyServer server;

    private static final String SESSION_ID_LISTENER_JAR_NAME = "SessionIdListener";
    private static final String TEST_SERVLET_31_JAR_NAME = "TestServlet31";
    private static final String TEST_SERVLET_31_APP_NAME = "TestServlet31";

    private static final String PRIVATE_HEADERS_APP_NAME = "PrivateHeadersTestServlet";

    private static final String PRIVATE_HEADERS_TEST_SERVLET_URL = "/TestServlet31/PrivateHeadersTestServlet";

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war apps and add the dependencies
        JavaArchive SessionIdListenerJar = ShrinkHelper.buildJavaArchive(SESSION_ID_LISTENER_JAR_NAME + ".jar",
                                                                         "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.listeners",
                                                                         "com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.servlets");
        JavaArchive TestServlet31Jar = ShrinkHelper.buildJavaArchive(TEST_SERVLET_31_JAR_NAME + ".jar",
                                                                     "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.jar.servlets");
        // Build the war app and add the dependencies
        WebArchive TestServlet31App = ShrinkHelper.buildDefaultApp(TEST_SERVLET_31_APP_NAME + ".war",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets",
                                                                   "com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.listeners");
        TestServlet31App = (WebArchive) ShrinkHelper.addDirectory(TestServlet31App, "test-applications/TestServlet31.war/resources");
        TestServlet31App = TestServlet31App.addAsLibraries(SessionIdListenerJar, TestServlet31Jar);

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, TestServlet31App);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(PrivateHeaderTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE9010E:.*", "SRVE9004E:.*", "SRVE8015E:.*", "SRVE9008E:.*", "SRVE9006E:.*", "SRVE8015E:.*");
        }
    }

    /*
     * Test sends a valid $WSSC Private header with the value https in a request
     * After Liberty processes this header, since it's valid, it gets stored away.
     * The servlet then uses the getScheme() method to retrieve the scheme for this request.
     * We expect to get this same value back in the response
     */

    @Test
    public void test_SendValid$WSSCPrivateHeader() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | PrivateHeaderTest]: test_SendValid$WSSCPrivateHeader");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + PRIVATE_HEADERS_TEST_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "scheme=https";

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod(request);
            post.setRequestHeader("$WSSC", "https");
            post.setRequestHeader("TestToCall", "test_SendValid$WSSCPrivateHeader");
            LOG.info("\n [TestRequestProperty]: test_SendValid$WSSCPrivateHeader");

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();

            assertTrue(request + " : The response did not contain the expected data. Response = " + ResponseBody + ", Expected data = " + ExpectedData,
                       ResponseBody.contains(ExpectedData));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: SendValid$WSSCPrivateHeader");
            LOG.info("\n /************************************************************************************/");
        }

    }

    /*
     * Test to make sure an invalid $WSSC header is not used
     * This test sends a bad scheme value into the server with the $WSSC private header.
     * Liberty processes this header, and shoudl discard it since it's invalid.
     * The servlet call the getScheme() method and it should return http and not the invalid scheme.
     */

    @Test
    public void test_SendInvalid$WSSCPrivateHeader() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | PrivateHeaderTest]: test_SendInvalid$WSSCPrivateHeader");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + PRIVATE_HEADERS_TEST_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "scheme=http";

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod(request);
            post.setRequestHeader("$WSSC", "https://badscheme/#");
            post.setRequestHeader("TestToCall", "test_SendInvalid$WSSCPrivateHeader");
            LOG.info("\n [TestRequestProperty]: test_SendInvalid$WSSCPrivateHeader");

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();
            assertTrue(request + " : The response did not contain the expected data. Response = " + ResponseBody + ", Expected data = " + ExpectedData,
                       ResponseBody.contains(ExpectedData));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: SendInvalid$WSSCPrivateHeader");
            LOG.info("\n /************************************************************************************/");
        }

    }

    /*
     * test to make sure an invalid $WSPR header is not used
     * An invalid protocol is sent to the server in the $WSPR protocol version private header.
     * The server should process the header and discard the invalid data.
     * When this private header is invalid, the value in the http request should be returned, http/1.1.
     */

    @Test
    public void test_SendValid$WSPRPrivateHeader() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | PrivateHeaderTest]: test_SendValid$WSPRPrivateHeader");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + PRIVATE_HEADERS_TEST_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "version=HTTP/1.1";

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod(request);
            post.setRequestHeader("$WSPR", "HTTP/1.1");
            post.setRequestHeader("TestToCall", "test_SendValid$WSPRPrivateHeader");
            LOG.info("\n [TestRequestProperty]: test_SendValid$WSPRPrivateHeader");

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();

            assertTrue(request + " : The response did not contain the expected data. Response = " + ResponseBody + ", Expected data = " + ExpectedData,
                       ResponseBody.contains(ExpectedData));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: SendValid$WSPRPrivateHeader");
            LOG.info("\n /************************************************************************************/");
        }

    }

    /*
     * test to make sure an invalid $WSPR header is not used
     * An invalid protocol is sent to the server in the $WSPR protocol version private header.
     * The server should process the header and discard the invalid data.
     * When this private header is invalid, the value in the http request should be returned, http/1.1.
     */

    @Test
    public void test_SendInvalid$WSPRPrivateHeader() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | PrivateHeaderTest]: test_SendInvalid$WSPRPrivateHeader");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + PRIVATE_HEADERS_TEST_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "version=HTTP/1.1";

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod(request);
            post.setRequestHeader("$WSPR", "http/1.27");
            post.setRequestHeader("TestToCall", "test_SendInvalid$WSPRPrivateHeader");
            LOG.info("\n [TestRequestProperty]: test_SendInvalid$WSPRPrivateHeader");

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();

            assertTrue(request + " : The response did not contain the expected data. Response = " + ResponseBody + ", Expected data = " + ExpectedData,
                       ResponseBody.contains(ExpectedData));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: SendInvalid$WSPRPrivateHeader");
            LOG.info("\n /************************************************************************************/");
        }

    }

    /*
     * test to make sure an invalid $WSRA header is not used
     * An invalid ipv6 address is sent to the server in the $WSRA remote address private header.
     * The server should discard this invalid address and use the client address instead.
     * The test makes sure that the invalid address is not returned.
     *
     */

    @Test
    public void test_SendValid$WSRAPrivateHeader() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | PrivateHeaderTest]: test_SendValid$WSPRPrivateHeader");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + PRIVATE_HEADERS_TEST_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String ExpectedData = "200.200.200.200";

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod(request);
            post.setRequestHeader("$WSRA", "200.200.200.200");
            post.setRequestHeader("TestToCall", "test_SendValid$WSRAPrivateHeader");
            LOG.info("\n [TestRequestProperty]: test_SendValid$WSRAPrivateHeader");

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();
            assertTrue(request + " : The response did not contain the expected data. Response = " + ResponseBody + ", Expected data = " + ExpectedData,
                       ResponseBody.contains(ExpectedData));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: SendValid$WSRAPrivateHeader");
            LOG.info("\n /************************************************************************************/");
        }

    }

    /*
     * test to make sure an invalid $WSRA header is not used
     * An invalid ipv6 address is sent to the server in the $WSRA remote address private header.
     * The server should discard this invalid address and use the client address instead.
     * The test makes sure that the invalid address is not returned.
     *
     */

    @Test
    public void test_SendInvalid$WSRAPrivateHeader() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | PrivateHeaderTest]: test_SendInvalid$WSPRPrivateHeader");
        LOG.info("\n /************************************************************************************/");

        String request = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + PRIVATE_HEADERS_TEST_SERVLET_URL;
        StringBuilder sb = new StringBuilder();
        String line = "";
        final String UnExpectedData = "[127::::1";

        try {

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod(request);
            post.setRequestHeader("$WSRA", "[127::::1");
            post.setRequestHeader("TestToCall", "test_SendInvalid$WSRAPrivateHeader");
            LOG.info("\n [TestRequestProperty]: test_SendInvalid$WSRAPrivateHeader");

            int ResponseCode = httpClient.executeMethod(post);
            LOG.info("Status Code = " + ResponseCode);

            String ResponseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + ResponseBody);

            post.releaseConnection();
            assertTrue(request + " : The response did contained the unexpected data. Response = " + ResponseBody + ", UnExpected data = " + UnExpectedData,
                       !ResponseBody.contains(UnExpectedData));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LOG.info("\n /************************************************************************************/");
            LOG.info("\n [WebContainer | AsyncReadListenerHttpUnit]: SendInvalid$WSRAPrivateHeader");
            LOG.info("\n /************************************************************************************/");
        }
    }

}