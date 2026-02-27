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
 * Test Request Cookie header according to RFC 6265:
 * 1. semicolon is the only delimiter; comma is discarded
 * 2. quotes are part of cookie value
 *
 * request URL: /Test61RequestCookieHeader?testName=xyz
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61RequestCookieHeaderTest {
    private static final Logger LOG = Logger.getLogger(Servlet61RequestCookieHeaderTest.class.getName());
    private static final String TEST_APP_NAME = "RequestCookieHeaderTest";

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
     * Cookie: $Version=1, name1=value1, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue
     *
     * All Cookie are discard since comma is used for all
     * Main data are in the response's headers
     */
    @Test
    public void test_Cookie_All_Comma_Delimiter() throws Exception {
        LOG.info(">>>>> test_Cookie_All_Comma_Delimiter <<<<<<");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/Test61RequestCookieHeader?testName=test_Cookie_All_Comma_Delimiter";
        LOG.info("Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);
        String EXPECTED_TEXT = "Result [PASS]";

        //Cookie is null since all comma parts are discarded
        getMethod.addHeader("Cookie", "$Version=1, name1=value1, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String headerValue = response.getHeader("TestResult").getValue();
                LOG.info(" TestResult : " + headerValue);

                assertTrue("The response does not contain Result [PASS]. TestResult header [" + headerValue + "]", headerValue.contains(EXPECTED_TEXT));
            }
        }
    }

    /**
     * Test COOKIE header with mix comma and semicolon delimiters.
     * Original Cookie: $Version=1, name1=value1; middleSemiColonName=middleSemiColonValue, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue; endSemiColonName=endSemiColonValue
     *
     * All Cookie comma pair are discard.
     */
    @Test
    public void test_Cookie_Mix_Comma_Semicolon_Delimiter() throws Exception {
        LOG.info(">>>>> test_Cookie_Mix_Comma_Semicolon_Delimiter <<<<<<");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/Test61RequestCookieHeader?testName=test_Cookie_Mix_Comma_Semicolon_Delimiter";
        LOG.info("Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);
        String EXPECTED_TEXT = "Result [PASS]";

        getMethod.addHeader("Cookie", "$Version=1, name1=value1; middleSemiColonName=middleSemiColonValue, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue; endSemiColonName=endSemiColonValue");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String headerValue = response.getHeader("TestResult").getValue();
                LOG.info(" TestResult : " + headerValue);

                assertTrue("The response does not contain Result [PASS]. TestResult header [" + headerValue + "]", headerValue.contains(EXPECTED_TEXT));
            }
        }
    }

    /**
     * Test Request Cookie header with quote
     * Original Cookie: "$Version=1; badDouble\"InName=NO; test_SingleQuote'In_Name=YES; \"badDouble2_InName\"=NO; test_MixQuotes_InValue_Name=\"Mix_Double'And'Single_Quotes_Name_YES\"; test_SingleQuote_InValue_Name='Single_Quote_Value_YES'; test_NoQuote_Name=NoQuoteValue_YES, quotedName2=\"To_Be_Removed_Pair_NO\"; test_DoubleQuote_InValue_Name=\"DoubleInValue_YES\"; badDouble\"ENDInName=NO");
     *
     * Invalid cookie name (with quotes) is at begin, middle, and end position in the Cookie list
     *
     * Cookie Name:
     *  No - Any double quote
     *  Yes - Single quote
     *
     * Cookie Value:
     *  Yes - Double and Single
     */
    @Test
    public void test_Cookie_Quoted_Value() throws Exception {
        LOG.info(">>>>> test_Cookie_Quoted_Value <<<<<<");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/Test61RequestCookieHeader?testName=test_Cookie_Quoted_Value";
        LOG.info("Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);
        String EXPECTED_TEXT = "Result [PASS]";

        getMethod.addHeader("Cookie", "$Version=1; badDouble\"InName=INVALID; test_SingleQuote'In_Name=YES; \"badDouble2_InName\"=INVALID; test_MixQuotes_InValue_Name=\"Mix_Double'And'Single_Quotes_Name_YES\"; test_SingleQuote_InValue_Name='Single_Quote_Value_YES'; test_NoQuote_Name=NoQuoteValue_YES, quotedAndComma_Name=\"Comma_pair_INVALID\"; test_DoubleQuote_InValue_Name=\"DoubleInValue_YES\"; badDouble\"ENDInName=NO");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String headerValue = response.getHeader("TestResult").getValue();
                LOG.info(" TestResult : " + headerValue);

                assertTrue("The response does not contain Result [PASS]. TestResult header [" + headerValue + "]", headerValue.contains(EXPECTED_TEXT));
            }
        }
    }

    //@Test add this to Servlet 6.0 FAT as well
    /*
     * Expires attribute allows comma. Test to make sure it works accordingly
     */
    public void test_SetCookie_Expires_Attribute() throws Exception {

    }
}
