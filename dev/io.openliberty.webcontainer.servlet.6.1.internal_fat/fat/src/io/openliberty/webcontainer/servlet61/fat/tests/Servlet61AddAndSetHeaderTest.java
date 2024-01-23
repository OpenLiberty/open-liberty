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
 * Test response add/setHeader, add/setDateHeader, add/setIntHeader.
 *
 * Use the request header "runTest" to specify a test to run
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61AddAndSetHeaderTest {

    private static final Logger LOG = Logger.getLogger(Servlet61AddAndSetHeaderTest.class.getName());
    private static final String TEST_APP_NAME = "AddAndSetHeaderTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";

    @Server("servlet61_AddAndSetHeaderTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_AddAndSetHeaderTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "headers.servlets");
        server.startServer(Servlet61AddAndSetHeaderTest.class.getSimpleName() + ".log");
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

    /**
     * Test response addHeader()
     * Test multiple value for same TEST-ADD-HEADER header.
     * Test header with an empty value.
     *
     * TEST-ADD-HEADER: ONE_addHeaderValue
     * TEST-ADD-HEADER: TWO_addHeaderValue
     */
    @Test
    public void test_Response_AddHeader() throws Exception {
        LOG.info("====== <test_Response_AddHeader> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestResponseHeaders";
        HttpGet getMethod = new HttpGet(url);
        String expectedString = "addHeaderValue";

        getMethod.addHeader("runTest", "testAddHeader");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                Header[] headers = response.getHeaders();
                String headerValue;
                int counter = 0;
                boolean isEmptyHeader = false;

                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");

                    //Test add a header with multiple values.
                    if (header.getName().equals("TEST-ADD-HEADER")) {
                        headerValue = header.getValue();
                        LOG.info("[TEST-ADD-HEADER] header ; value [" + headerValue + "] ...expecting 2 headers.");
                        if (headerValue.equals("ONE_addHeaderValue") || headerValue.equals("TWO_addHeaderValue")) {
                            counter++;

                            if (counter == 2)
                                LOG.info("[TEST-ADD-HEADER] - found 2 headers.");
                        }
                    }

                    //Test add with empty value
                    if (header.getName().equals("TEST-ADD-HEADER_EMPTY-VALUE")) {
                        headerValue = header.getValue();
                        LOG.info("[TEST-ADD-HEADER_EMPTY-VALUE] header; value [" + headerValue + "] ...expecting an empty string");
                        if (headerValue.isEmpty()) {
                            isEmptyHeader = true;
                        }
                    }
                }

                assertTrue("Expecting 2 [TEST-ADD-HEADER] headers but found [" + counter + "]", counter == 2);
                assertTrue("Expecting [TEST-ADD-HEADER_EMPTY-VALUE] with empty value but not", isEmptyHeader);
            }
        }
    }

    /*
     * Test response setHeader()
     *
     * setHeader("TEST-SET-HEADER", null); will remove the header entirely; so don't expect this header
     * setHeader, with a value, remove ALL previous values for the same header.
     *
     * TEST-MULTI-ADD-THEN-SET-HEADER: FINAL_BY_SET_HeaderValue
     */
    @Test
    public void test_Response_SetHeader() throws Exception {
        LOG.info("====== <test_Response_SetHeader> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestResponseHeaders";
        HttpGet getMethod = new HttpGet(url);
        Header header;

        getMethod.addHeader("runTest", "testSetHeader");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                //set TEST-SET-HEADER header to a value; then set again with null value which will remove this header.
                header = response.getHeader("TEST-SET-HEADER");
                assertTrue("Not expecting [TEST-SET-HEADER] but Found [" + header + "]", header == null);

                //Test add [TEST-ADD-THEN-SET-HEADER-NULL-VALUE] header with multiple values; then set same header to null;
                //Expect: this header is not in the response.
                header = response.getHeader("TEST-ADD-THEN-SET-HEADER-NULL-VALUE");
                assertTrue("Not expecting [TEST-ADD-THEN-SET-HEADER-NULL-VALUE] but Found [" + header + "]", header == null);

                //Test set with empty value
                header = response.getHeader("TEST-SET-HEADER_EMPTY-VALUE");
                if (header != null) {
                    String headerValue = header.getValue();
                    LOG.info("Found [TEST-SET-HEADER_EMPTY-VALUE] header: value [" + headerValue + "] , expecting an empty string");
                    assertTrue("Expecting [TEST-SET-HEADER_EMPTY-VALUE] with empty value but found [" + headerValue + "]", headerValue.isEmpty());
                } else {
                    fail("[TEST-SET-HEADER_EMPTY-VALUE] header NOT found");
                }

                LOG.info("Iterate through headers ....");
                Header[] headers = response.getHeaders();
                String headerValue;
                for (Header _header : headers) {
                    LOG.info("Found [" + _header + "]");

                    //add a header with multiple value; then set same header with different value which replaces ALL previous values
                    if (_header.getName().equals("TEST-MULTI-ADD-THEN-SET-HEADER")) {
                        headerValue = _header.getValue();
                        LOG.info("[TEST-MULTI-ADD-THEN-SET-HEADER] header ; value [" + headerValue + "] , expecting value [FINAL_BY_SET_HeaderValue]");

                        assertTrue("Expecting [TEST-MULTI-ADD-THEN-SET-HEADER] header with value [FINAL_BY_SET_HeaderValue] but found [" + headerValue + "]",
                                   headerValue.equals("FINAL_BY_SET_HeaderValue"));
                    }
                }
            }
        }
    }

    /*
     * Test add/setDateHeader
     *
     * TEST-ADD-DATE-HEADER: Fri, 30 Dec 2022 19:05:51 GMT
     * TEST-ADD-DATE-HEADER: Sat, 30 Dec 2023 19:05:51 GMT
     *
     * TEST-SET-DATE-HEADER: Tue, 09 Jan 2024 00:31:55 GMT
     *
     * TEST-MULTI-ADD-THEN-SET-DATE-HEADER: Tue, 09 Jan 2024 00:31:55 GMT
     */
    @Test
    public void test_Response_AddandSetDateHeader() throws Exception {
        LOG.info("====== <test_Response_AddandSetDateHeader> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestResponseHeaders";
        HttpGet getMethod = new HttpGet(url);
        Header header;
        String headerValue;

        getMethod.addHeader("runTest", "testAddandSetDateHeader");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                Header[] headers = response.getHeaders();
                int counter = 0;
                boolean foundAddDateHeader = false;

                LOG.info("Iterate through headers ....");
                for (Header _header : headers) {
                    LOG.info("Found Header: [" + _header + "]");

                    /*
                     * Test a header with multiple values.
                     * TEST-ADD-DATE-HEADER: Fri, 30 Dec 2022 19:05:51 GMT
                     * TEST-ADD-DATE-HEADER: Sat, 30 Dec 2023 19:05:51 GMT
                     */
                    if (_header.getName().equals("TEST-ADD-DATE-HEADER")) {
                        foundAddDateHeader = true;
                        headerValue = _header.getValue();
                        LOG.info("[TEST-ADD-DATE-HEADER] header ; value [" + headerValue + "] ...expecting 2 of the same headers with different values.");
                        if (headerValue.contains("2022") || headerValue.contains("2023")) {
                            counter++;

                            if (counter == 2)
                                LOG.info("[TEST-ADD-DATE-HEADER] - found 2 headers.");
                        }
                    }

                    //Test add a header with multiple values; then setDateHeader with a value which override ALL previous values for the same header
                    if (_header.getName().equals("TEST-MULTI-ADD-THEN-SET-DATE-HEADER")) {
                        headerValue = _header.getValue();
                        LOG.info("[TEST-MULTI-ADD-THEN-SET-DATE-HEADER] header; value [" + headerValue + "] ...expecting value contains 2024 .");

                        assertTrue("Expecting [TEST-MULTI-ADD-THEN-SET-DATE-HEADER] header contains 2024 not found ", headerValue.contains("2024"));
                    }
                }
                assertTrue("Expecting 2 [TEST-ADD-DATE-HEADER] headers but found [" + counter + "]", (counter == 2 && foundAddDateHeader));

                //Test setDateHeader
                header = response.getHeader("TEST-SET-DATE-HEADER");
                if (header != null) {
                    headerValue = header.getValue();
                    LOG.info("[TEST-SET-DATE-HEADER] header ; value [" + headerValue + "] ...expecting value contains 2024 string.");

                    assertTrue("Expecting [TEST-SET-DATE-HEADER] containers 2024 but not found  ", headerValue.contains("2024"));
                } else
                    fail("[TEST-SET-DATE-HEADER] header NOT found");

            }
        }
    }

    /*
     * Expecting headers:
     *
     * TEST-ADD-INT-HEADER: 12345
     * TEST-ADD-INT-HEADER: 67890
     *
     * TEST-SET-INT-HEADER: 98765
     *
     * TEST-MULTI-ADD-THEN-SET-INT-HEADER: 888888
     */
    @Test
    public void test_Response_AddandSetIntHeader() throws Exception {
        LOG.info("====== <test_Response_AddandSetIntHeader> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestResponseHeaders";
        HttpGet getMethod = new HttpGet(url);
        Header header;
        String headerValue;
        boolean foundAddIntHeader = false;

        getMethod.addHeader("runTest", "testAddandSetIntHeader");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                LOG.info("Iterate through headers ....");
                Header[] headers = response.getHeaders();
                int counter = 0;
                for (Header _header : headers) {
                    LOG.info("Found Header: [" + _header + "]");

                    //Test add a header with multiple values.
                    if (_header.getName().equals("TEST-ADD-INT-HEADER")) {
                        foundAddIntHeader = true;
                        headerValue = _header.getValue();
                        LOG.info("[TEST-ADD-INT-HEADER] header ; value [" + headerValue + "] ...expecting 2 of the same headers with different values.");
                        if (headerValue.equals("12345") || headerValue.equals("67890")) {
                            counter++;

                            if (counter == 2)
                                LOG.info("[TEST-ADD-INT-HEADER] - found 2 headers.");
                        }
                    }

                    //Test add a header with multiple values; then setIntHeader which override ALL previous values for the same header
                    if (_header.getName().equals("TEST-MULTI-ADD-THEN-SET-INT-HEADER")) {
                        headerValue = _header.getValue();
                        LOG.info("[TEST-MULTI-ADD-THEN-SET-INT-HEADER] header; value [" + headerValue + "] ...expecting value [888888]");

                        assertTrue("Expecting [TEST-MULTI-ADD-THEN-SET-INT-HEADER] header with value [888888] but found [" + headerValue + "]", headerValue.equals("888888"));
                    }
                }

                assertTrue("Expecting 2 [TEST-ADD-INT-HEADER] headers but found [" + counter + "]", (counter == 2 && foundAddIntHeader));

                //Test setIntHeader
                header = response.getHeader("TEST-SET-INT-HEADER");
                if (header != null) {
                    headerValue = header.getValue();
                    LOG.info("[TEST-SET-INT-HEADER] header ; value [" + headerValue + "] ...expecting value [98765]");

                    assertTrue("Expecting [TEST-SET-INT-HEADER] with value [98765] but found [" + headerValue + "]", headerValue.equals("98765"));
                } else
                    fail("[TEST-SET-INT-HEADER] header NOT found");
            }
        }
    }

    @Test
    public void test_Request_GetDateAndIntHeaders() throws Exception {
        LOG.info("====== <test_Request_GetDateAndIntHeaders> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestResponseHeaders";
        HttpGet getMethod = new HttpGet(url);

        final String DATE_2022 = "Fri, 30 Dec 2022 19:05:51 GMT";
        final String DATE_2023 = "Sat, 30 Dec 2023 19:05:51 GMT";
        final String DATE_2024 = "Tue, 09 Jan 2024 00:31:55 GMT";


        getMethod.addHeader("runTest", "testRequestGetDateAndIntHeaders");

        //send same request header with multiple values
        getMethod.addHeader("testInHeader" , "61");
        getMethod.addHeader("testInHeader" , "60");
        getMethod.addHeader("testInHeader" , "50");

        getMethod.addHeader("testDateHeader" , DATE_2022);
        getMethod.addHeader("testDateHeader" , DATE_2024);

        getMethod.addHeader("If-Modified-Since" , DATE_2023);
        getMethod.addHeader("If-Modified-Since" , DATE_2024);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    LOG.info("Found Header: [" + header + "]");
                }

                String value = response.getHeader("requestInHeader").getValue();
                assertTrue("Expecting requestIntHeader is [61]. Found [" + value + "]", value.equals("61"));

                value = response.getHeader("requestDateHeader").getValue();
                assertTrue("Expecting requestDateHeader is ["+ DATE_2022 + "]. Found [" + value + "]", value.equals(DATE_2022));

                value = response.getHeader("requestIfModifiedSinceHeader").getValue();
                assertTrue("Expecting requestIfModifiedSinceHeader is ["+ DATE_2023 + "]. Found [" + value + "]", value.equals(DATE_2023));
            }
        }
    }
}
