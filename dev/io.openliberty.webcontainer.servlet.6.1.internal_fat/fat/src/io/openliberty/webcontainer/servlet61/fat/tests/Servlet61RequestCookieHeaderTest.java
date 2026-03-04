/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
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
 * Test RFC 6265 Request Cookie header:
 * 1. semicolon is the only delimiter; comma is discarded
 *
 * request URL: /Test61RequestCookieHeader?testName=xyz
 *
 * Quoted tests are in 60 bucket
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61RequestCookieHeaderTest {
    private static final Logger LOG = Logger.getLogger(Servlet61RequestCookieHeaderTest.class.getName());
    private static final String TEST_APP_NAME = "RequestCookieHeader61Test";

    @Server("servlet61_RequestCookieHeaderTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "requestcookieheader.servlets");

        server.startServer(Servlet61RequestCookieHeaderTest.class.getSimpleName() + ".log");
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
     * Test COOKIE header with All comma delimiter.
     *  $Version=1, name1=value1, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue";
     *
     * All Cookie are discard since comma is used for all. getCookie() return null
     */
    @Test
    public void test_Cookie_All_Comma_Delimiter() throws Exception {
        LOG.info(">>>>> test_Cookie_All_Comma_Delimiter <<<<<<");

        String testName = "test_Cookie_All_Comma_Delimiter";
        String cookieHeader = "$Version=1, name1=value1, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue";


        sendRequest(testName, cookieHeader);
    }

    /**
     * Test COOKIE header with mix comma and semicolon delimiters.
     * Cookie:
     * "$Version=1, comma_Name1=Invalid; middleSemiColonName=middleSemiColonValue, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue; endSemiColon_Name=endSemiColonValue, comma_Name2=Invalid";
     *
     * All Cookie comma pair are discard.
     */
    @Test
    public void test_Cookie_Mix_Comma_Semicolon_Delimiter() throws Exception {
        LOG.info(">>>>> test_Cookie_Mix_Comma_Semicolon_Delimiter <<<<<<");

        String testName = "test_Cookie_Mix_Comma_Semicolon_Delimiter";
        String cookieHeader = "$Version=1, comma_Name1=Invalid; middleSemiColonName=middleSemiColonValue, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue; endSemiColon_Name=endSemiColonValue, comma_Name2=Invalid";

        sendRequest(testName, cookieHeader);
    }

    /*
     * application servlet will verify the cookies and response PASS or FAIL.
     */
    private void sendRequest(String urlPattern, String cookieHeader) throws Exception {
        String EXPECTED_TEXT = "Result [PASS]";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/Test61RequestCookieHeader?testName=" + urlPattern;

        LOG.info("Sending Request [" + url + "]");
        LOG.info("Request Cookie [" + cookieHeader + "]");

        HttpGet getMethod = new HttpGet(url);
        getMethod.addHeader("Cookie", cookieHeader);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String headerValue = response.getHeader("TestResult").getValue();
                LOG.info(" TestResult : " + headerValue);
                assertTrue("The response does not contain Result [PASS]. TestResult header [" + headerValue + "]", headerValue.contains(EXPECTED_TEXT));
            }
        }
    }
}
