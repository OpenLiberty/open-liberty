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
 * Test ServletRequest getRequestID and ServletConnection API
 *
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getRequestId()
 *
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection
 *
 * request URL: /TestServletRequestID
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60RequestConnectionTest {

    private static final Logger LOG = Logger.getLogger(Servlet60RequestConnectionTest.class.getName());
    private static final String TEST_APP_NAME = "RequestConnectionTest";
    private static final String expectedResponse = "Test ServletRequest and ServletConnection";

    @Server("servlet60_requestConnectionTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "requestconnection.servlets");

        server.startServer(Servlet60RequestConnectionTest.class.getSimpleName() + ".log");
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
     * Test servlet request ID which is unique per server's life cycle.
     * Test ServletConnection APIs.
     *
     * Keep in one test since it expects request ID = 1.
     * (i.e there is no guarantee of @ Test order if there are multiple Tests)
     *
     * Main data are in the response's headers
     *
     */
    @Test
    public void test_requestConnection() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestServletRequestID";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                //fail-fast
                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));

                Header[] headers = response.getHeaders();
                String headerValue;
                for (Header header : headers) {
                    LOG.info("\n===============================");
                    LOG.info("\n" + "Header: [" + header + "]");

                    /*
                     * ServletRequest
                     */

                    if (header.getName().equals("req.getRequestId")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting [1].");

                        assertTrue("Expecting getRequestId is 1 , but found [" + headerValue + "]", headerValue.equals("1"));
                    }

                    //HTTP/1.1 - ProtocolRequestId is empty
                    if (header.getName().equals("req.getProtocolRequestId")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting empty [].");

                        assertTrue("Expecting empty ProtocolRequestId, but found [" + headerValue + "]", headerValue.isBlank());
                    }

                    //Verify that value contains "io.openliberty.webcontainer60.srt.SRTServletConnection" ..do not use .equals since there is object id at the end.
                    if (header.getName().equals("req.getServletConnection")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting [io.openliberty.webcontainer60.srt.SRTServletConnection].");

                        assertTrue("Expecting SRTServletConnection , but found [" + headerValue + "]",
                                   headerValue.contains("io.openliberty.webcontainer60.srt.SRTServletConnection"));
                    }

                    /*
                     * ServletConnection
                     */

                    //Connection Id is also expecting to be 1 for first request
                    if (header.getName().equals("conn.getConnectionId")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting [1].");

                        assertTrue("Expecting getConnectionId is 1 , but found [" + headerValue + "]", headerValue.equals("1"));
                    }

                    if (header.getName().equals("conn.getProtocol")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting [HTTP/1.1].");

                        assertTrue("Expecting getProtocol is HTTP/1.1 , but found [" + headerValue + "]", headerValue.equals("HTTP/1.1"));
                    }

                    // https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection#getProtocolConnectionId()
                    if (header.getName().equals("conn.getProtocolConnectionId")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting empty [].");

                        assertTrue("Expecting empty getProtocolConnectionId , but found [" + headerValue + "]", headerValue.isBlank());
                    }

                    if (header.getName().equals("conn.isSecure")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Header value [" + headerValue + "] . Expecting [false].");

                        assertTrue("Expecting isSecure is false , but found [" + headerValue + "]", headerValue.equals("false"));
                    }
                }

            }
        }
    }
}
