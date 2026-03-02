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
import org.apache.hc.core5.http.Header;
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
     *  $Version=1, name1=value1; middleSemiColonName=middleSemiColonValue, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue; endSemiColonName=endSemiColonValue
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

    /**
     * Test Request Cookie header with quote
     *
     * Cookie:
     *  "$Version=1; badDouble\"InName=InValid; test_SingleQuote'In_Name=Valid; \"badDouble2_InName\"=InValid; test_MixQuotes_InValue_Name=\"Mix_Double'And'Single_Quotes_Name_Valid\"; test_SingleQuote_InValue_Name='Single_Quote_Value_Valid'; test_NoQuote_Name=NoQuoteValue_Valid; test_2DoubleQuote_InValue_Name=\"DoubleInValue_Valid\"; badDouble\"ENDInName=InValid";

     * Invalid cookie name (with double quotes) is at begin, middle, and end position in the Cookie list:
     *
     * Cookie Name:
     *  No - Double quote anywhere
     *  Yes - Single quote
     *
     * Cookie Value:
     *  Yes - Double and Single
     */
    @Test
    public void test_Cookie_Quoted_Value() throws Exception {
        LOG.info(">>>>> test_Cookie_Quoted_Value <<<<<<");

        String testName = "test_Cookie_Quoted_Value";
        String cookieHeader = "$Version=1; badDouble\"InName=InValid, comma_Name=Double\"InValue_Invalid; test_SingleQuote'In_Name=Valid; \"badDouble2_InName\"=InValid; test_MixQuotes_InValue_Name=\"Mix_Double'And'Single_Quotes_Name_Valid\"; test_SingleQuote_InValue_Name='Single_Quote_Value_Valid'; test_NoQuote_Name=NoQuoteValue_Valid; test_2DoubleQuote_InValue_Name=\"DoubleInValue_Valid\"; badDouble\"ENDInName=InValid";

        sendRequest(testName, cookieHeader);
    }

    /*
     * Test others cases:
     *  =noNameWithValue
     *  nameWithEmptyValue=""
     *  nameWithoutAnyValue=
     *  nameOnly
     *  endingSemiName=NoPairAfterSemi;
     */
    @Test
    public void test_Cookie_Other() throws Exception {
        LOG.info(">>>>> test_Cookie_Other <<<<<<");

        String testName = "test_Cookie_Other";
        String cookieHeader = "$Version=1; =noNameWithValue; nameWithEmptyValue=\"\"; nameWithoutAnyValue=; nameOnly; endingSemiName=NoPairAfterSemi;";

        sendRequest(testName, cookieHeader);
    }

    /*
     * Test Response Set-Cookie: Servlet generates several Set-Cookie headers
     *
     * 1. the comma in the Response Set-Cookie ; Expires attribute.
     * 2. Quotes in both name and value
     *
     * Any Set-Cookie contains "InValid" fails this test.
     * Check for ; Expires in one Set-Cookie "manualSetCookie_Valid_Name"
     */
    @Test
    public void test_Request_Set_Cookie_Name_Expires_Attribute() throws Exception {
        LOG.info(">>>>> test_Request_Set_Cookie_Name_Expires_Attribute <<<<<<");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/Test61RequestCookieHeader?testName=test_Request_Set_Cookie_Name_Expires_Attribute";
        LOG.info("Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String value = null;
            final String EXPIRES = "; Expires=Sat, 01 Mar 2036"; //do not include time
            final String OTHER_ATT = "; HttpOnly";
            int validCookieCounter = 0;

            try (CloseableHttpResponse response = client.execute(getMethod)) {
                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    if (header.getName().equals("Set-Cookie")){
                        LOG.info(header.toString());

                        value = header.getValue();
                        if (value.contains("InValid")) {
                            assertTrue("Response Set-Cookie has InValid string [" + value + "]", false);
                        }

                        /*
                         * This Set-Cookie was set as
                         * String manualSetCookie2 = "cookie2_Quote'Name=Value_Valid, cookie2_Comma_Pair=Invalid; Expires=Sat, 01 Mar 2036 19:00:00 GMT; HttpOnly";
                         *
                         * Verify the comma skipping and ;Expires are parsed correctly.
                         */
                        if (value.contains("cookie2_Quote'Name=Value_Valid")) {
                            assertTrue("Response Set-Cookie [cookie2_Quote'Name=Value_Valid] does not contain [" + EXPIRES + "] AND [" + OTHER_ATT + "]", value.contains(EXPIRES) && value.contains(OTHER_ATT));
                        }
                        validCookieCounter++;
                    }
                }

                //2nd check - Expect 5 valid Set-Cookie
                assertTrue("Response Set-Cookie: Expected 5 valid Set-Cookie, but found [" + validCookieCounter + "]", validCookieCounter == 5);
            }
        }
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
