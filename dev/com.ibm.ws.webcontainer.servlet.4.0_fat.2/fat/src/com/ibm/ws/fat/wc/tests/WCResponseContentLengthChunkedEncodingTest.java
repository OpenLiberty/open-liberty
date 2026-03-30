/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
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
 * Test response Content-Length VS Chunked Encoding when using Writer (response.getWriter)
 * Also test some of the getOutputStream.
 * DBCS is also tested
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCResponseContentLengthChunkedEncodingTest {
    private static final Logger LOG = Logger.getLogger(WCResponseContentLengthChunkedEncodingTest.class.getName());
    private static final String APP_NAME = "ResponseContentLengthChunkedEncoding";
    private static final String SERVLET_PATH = "responechunkedencoding";

    @Server("servlet40_responseCLChunkEncoding")
    public static LibertyServer server;

    private String testQueryString = null;
    private static final String LENGTH = "Content-Length";
    private static final String TRANSFER_ENC = "Transfer-Encoding";

    //Use when debug locally!
    private static boolean DISPLAY_OFF = false;
    private static boolean DISPLAY_ON = true;

    //Assert response data which always begins with BEGIN_ and end with _END
    private static String BEGIN = "BEGIN_";
    private static String END = "_END";
    private static String SECOND_END = "_SECOND_SET_DATA_END";

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup server: servlet40_responseCLChunkEncoding");
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "response.servlets");
        server.startServer(WCResponseContentLengthChunkedEncodingTest.class.getSimpleName() + ".log");
        LOG.info("Setup: startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Test small size ASCII
     * Type - Writer
     * Expecting - Content-Length
     */
    @Test
    public void test_Writer_Small_ASCII() throws IOException, ParseException {
        testQueryString = "data_size=2000";

        LOG.info("====== <test_Writer_Small_ASCII [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 2010, null, DISPLAY_OFF);
    }

    /*
     * Test small size ASCII
     * Type - Stream
     * Expecting - Content-Length
     */
    @Test
    public void test_Stream_Small_ASCII() throws IOException, ParseException {
        testQueryString = "data_size=2000&write_type=out";

        LOG.info("====== <test_Stream_Small_ASCII [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 2010, null, DISPLAY_OFF);
    }

    /*
     * Test large size ASCII that overflow the buffer
     * Type: Writer
     * Expecting - Chunked
     */
    @Test
    public void test_Writer_Large_ASCII() throws IOException, ParseException {
        testQueryString = "data_size=33000";

        LOG.info("====== <test_Writer_Large_ASCII [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test large size ASCII that overflow the buffer
     * Type - Stream
     * Expecting - Chunked
     */
    @Test
    public void test_Stream_Large_ASCII() throws IOException, ParseException {
        testQueryString = "data_size=33000&write_type=out";

        LOG.info("====== <test_Stream_Large_ASCII [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test large size ASCII that overflow the buffer but explicitly setContentLength
     * Type - Writer
     * Expecting - CL
     */
    @Test
    public void test_Writer_Large_ASCII_setCL() throws IOException, ParseException {
        testQueryString = "data_size=33000&set_contentlength=true";

        LOG.info("====== <test_Writer_Large_ASCII_setCL [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 33010, null, DISPLAY_OFF);
    }

    /*
     * Test large size ASCII that overflow the buffer but explicitly setContentLength
     * Type - Stream
     * Expecting - CL
     */
    @Test
    public void test_Stream_Large_ASCII_setCL() throws IOException, ParseException {
        testQueryString = "data_size=33000&set_contentlength=true&write_type=out";

        LOG.info("====== <test_Stream_Large_ASCII_setCL [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 33010, null, DISPLAY_OFF);
    }

    /*
     * Test medium DBCS 15000 that is below bufferSize; however, after encoded, it becomes (1500 * 3) = 45000 will be over bufferSize
     * Type: Writer
     * Expecting - Chunked
     */
    @Test
    public void test_Writer_Medium_DBCS() throws IOException, ParseException {
        testQueryString = "data_size=15000&multibyte=true";

        LOG.info("====== <test_Writer_Medium_DBCS [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test medium DBCS 15000 that is below bufferSize; however, after encoded, it becomes 45000 will be over bufferSize
     * Type: Stream
     * Expecting - Chunked
     */
    @Test
    public void test_Stream_Medium_DBCS() throws IOException, ParseException {
        testQueryString = "data_size=15000&multibyte=true&write_type=out";

        LOG.info("====== <test_Stream_Medium_DBCS [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS. The final size is expanded after encoded
     * Type - Writer
     * Expecting - Content-Length
     */
    @Test
    public void test_Writer_Small_DBCS() throws IOException, ParseException {
        testQueryString = "data_size=2000&multibyte=true";

        LOG.info("====== <test_Writer_Small_DBCS [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 6010, null, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS. The final size is expanded after encoded
     * Type - Stream
     * Expecting - Content-Length
     */
    @Test
    public void test_Stream_Small_DBCS() throws IOException, ParseException {
        testQueryString = "data_size=2000&multibyte=true&write_type=out";

        LOG.info("====== <test_Stream_Small_DBCS [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 6010, null, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS and response.flushBuffer()
     * Type - Writer
     * Expecting - Chunked
     */
    @Test
    public void test_Writer_Small_DBCS_flush() throws IOException, ParseException {
        testQueryString = "data_size=2000&multibyte=true&force_flush=true";

        LOG.info("====== <test_Writer_Small_DBCS_flush [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test medium size DBCS , flushBuffer, send 2nd set data
     * Type - Writer
     * Expecting - Chunked and Additional END data
     */
    @Test
    public void test_Writer_DBCS_multi_send() throws IOException, ParseException {
        testQueryString = "data_size=11000&multibyte=true&multi_send=true";

        LOG.info("====== <test_Writer_DBCS_multi_send [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, SECOND_END, DISPLAY_OFF);
    }

    /*
     * Test medium size DBCS , flush, send 2nd set data
     * Type - Stream
     * Expecting - Chunked and Additional END data
     */
    @Test
    public void test_Stream_DBCS_multi_send() throws IOException, ParseException {
        testQueryString = "data_size=11000&multibyte=true&multi_send=true&write_type=out";

        LOG.info("====== <test_Stream_DBCS_multi_send [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, SECOND_END, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS and setBufferSize(8192); the data size is expanded to 12012 which > new buffer 8192
     * Type - Writer
     * Expecting - Chunked
     */
    @Test
    public void test_Writer_DBCS_setBufferSize_8192() throws IOException, ParseException {
        testQueryString = "data_size=4000&multibyte=true&buffer_size=8192";

        LOG.info("====== <test_Writer_DBCS_setBufferSize_8192 [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS and setBufferSize(8192); the data size is expanded to 12012 which > new buffer 8192
     * Type - Stream
     * Expecting - Chunked
     */
    @Test
    public void test_Stream_DBCS_setBufferSize_8192() throws IOException, ParseException {
        testQueryString = "data_size=4000&multibyte=true&buffer_size=8192&write_type=out";

        LOG.info("====== <test_Stream_DBCS_setBufferSize_8192 [" + testQueryString + "] > ======");
        sendRequest(testQueryString, -1, null, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS and setBufferSize(16000); the data size is expanded to 12012 which fits within the setBufferSize
     * Type - Writer
     * Expecting - CL
     */
    @Test
    public void test_Writer_DBCS_setBufferSize_16000() throws IOException, ParseException {
        testQueryString = "data_size=4000&multibyte=true&buffer_size=16000";

        LOG.info("====== <test_Writer_DBCS_setBufferSize_16000 [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 12010, null, DISPLAY_OFF);
    }

    /*
     * Test small size DBCS and setBufferSize(16000); the data size is expanded to 12012 which fits within the setBufferSize
     * Type - Stream
     * Expecting - CL
     */
    @Test
    public void test_Stream_DBCS_setBufferSize_16000() throws IOException, ParseException {
        testQueryString = "data_size=4000&multibyte=true&buffer_size=16000";

        LOG.info("====== <test_Stream_DBCS_setBufferSize_16000 [" + testQueryString + "] > ======");
        sendRequest(testQueryString, 12010, null, DISPLAY_OFF);
    }

    /**
     * String queryString: test query<p>
     * int expectedSize: (-1 for TRANSFER_ENC) or (integer Content-Length size) <p>
     * String additionalExpectedString : additional expected string (besides "BEGIN_" and "_END" which are always assert on)<p>
     * boolean displayResponse = false when test with LARGE data!<p>
     */
    private void sendRequest(String queryString, int expectedSize, String additionalExpectedString,
                             boolean displayResponse) throws IOException, ParseException {
        int responseCL = -1;
        boolean chunkedResponse = false;
        String responseText = null;

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/" + SERVLET_PATH + "?" + queryString;
        LOG.info("Send Request: [" + url + "]");

        HttpGet getMethod = new HttpGet(url);
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                Header[] headers = response.getHeaders();

                LOG.info("\n>>>>>> Response Headers: >>>>>>");
                for (Header header : headers) {
                    LOG.info(header.toString());
                    if (header.getName().equals(LENGTH)) {
                        responseCL = Integer.valueOf(header.getValue());
                        LOG.info("\n Found Content-Length = " + responseCL);
                    }

                    if (header.getName().equals(TRANSFER_ENC)) {
                        chunkedResponse = true;
                        LOG.info("\n Found " + TRANSFER_ENC + " = " + header.getValue());
                    }
                }
                LOG.info("\n<<<<<< Response Headers <<<<<<");

                HttpEntity entity = response.getEntity();

                if (expectedSize > 0) {
                    assertTrue("Expected Content-Length size [" + expectedSize + "] but found [" + responseCL + "]", expectedSize == responseCL);

                    //check the actual body size against the response CL header:
                    long cl = entity.getContentLength();
                    byte[] body = EntityUtils.toByteArray(entity);
                    long actual = body.length;

                    LOG.info("CL header = " + cl + " . Actual body = " + actual);
                    if (cl != actual) {
                        assertFalse("Actual body data [" + actual + "] does not match CL header [" + cl + "]", false);
                    } else {
                        responseText = new String(body, StandardCharsets.UTF_8);
                    }

                } else { //TRANSFER_ENC
                    assertTrue("Expected [Transfer-Encoding: Chunked] not found", chunkedResponse);
                    responseText = EntityUtils.toString(entity);
                }

                if (displayResponse) {
                    LOG.info("\n" + "Response Text: \n[" + responseText + "]");
                }

                assertTrue("Expected string [" + BEGIN + "] not found in the response", responseText.startsWith(BEGIN));

                if (additionalExpectedString != null) {
                    assertTrue("Expected additional string [" + SECOND_END + "] not found in the response", responseText.endsWith(SECOND_END));
                } else {
                    assertTrue("Expected string [" + END + "] not found in the response", responseText.endsWith(END));
                }
            } catch (Exception ex) {
                LOG.info("Exception [" + ex.toString() + "]");
            }
        }
    }
}
