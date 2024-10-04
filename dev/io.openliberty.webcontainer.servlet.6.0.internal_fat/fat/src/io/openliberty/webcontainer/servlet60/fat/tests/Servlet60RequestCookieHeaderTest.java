/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Test Request Cookie header according to RFC 6265:
 * 1. Except $version, $ prefix any name will be part of the new cookie name (including $ sign).
 * That also applies to those special attributes like Domain, Path
 * 2. max-age=0 set by the application is expecting explicitly in the response Set-Cookie header
 *
 * request URL: /TestRequestCookieHeader?testName=xyz
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60RequestCookieHeaderTest {
    private static final Logger LOG = Logger.getLogger(Servlet60RequestCookieHeaderTest.class.getName());
    private static final String TEST_APP_NAME = "RequestCookieHeaderTest";

    @Server("servlet60_requestCookieHeaderTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "requestcookieheader.servlets");

        server.startServer(Servlet60RequestCookieHeaderTest.class.getSimpleName() + ".log");
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
     * Test combination of COOKIE header with mix name.
     * Request sends the following header to application:
     * Cookie: $Version=1; name1=value1; $Path=/Dollar_Path; $Domain=localhost; $NAME2=DollarNameValue; Domain=DomainValue
     *
     * Main data are in the response's headers
     *
     */
    @Test
    public void test_MixCookieNameWithDollarSigns() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestRequestCookieHeader?testName=MixCookieNames";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        //Set request header COOKIE
        //server is expecting to parse this header into 5 separate cookies
        getMethod.addHeader("Cookie", "$Version=1; name1=value1; $Path=/Dollar_Path; $Domain=localhost; $NAME2=DollarNameValue; Domain=DomainValue");

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
     * EE 10 expects both Max-Age=0 and Expires
     * EE 11 and others - expect Expires
     */
    @Test
    public void test_MaxAgeZero() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestRequestCookieHeader?testName=MaxAgeZero";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                String headerValue = response.getHeader("Set-Cookie").getValue();

                LOG.info("\n TestResult : " + headerValue);

                if (JakartaEEAction.isEE10Active()) {
                    assertTrue("The Set-Cookie response header does not contain attribute [Max-Age=0]. TestResult header [" + headerValue + "]", headerValue.contains("Max-Age=0"));
                } else if (JakartaEEAction.isEE11OrLaterActive()) {
                    assertTrue("The Set-Cookie response header contains unexpected attribute [Max-Age=0]. TestResult header [" + headerValue + "]",
                               !headerValue.contains("Max-Age=0"));
                }

                assertTrue("The Set-Cookie response header does not contain attribute [Expires]. TestResult header [" + headerValue + "]", headerValue.contains("Expires"));
            }
        }
    }
}
