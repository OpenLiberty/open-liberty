/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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

    /*
     * Test Response Set-Cookie:
     *  Quotes in name and value
     *  Comma in cookie-pair. EX: cookie2_Quote'Name=cookie2_Has_CommaTrailing_Value, cookie2_After_Comma_Name=cookie2_After_Comma_Value; Expires=Sat, 01 Mar 2036 19:00:00 GMT"
     */
    @Test
    public void test_Response_Set_Cookie_Name_Expires_Attribute() throws Exception {
        boolean ee10 = JakartaEEAction.isEE11OrLaterActive() ? false : true;

        LOG.info(">>>>> test_Response_Set_Cookie_Name_Expires_Attribute . " + (ee10 ? "Servlet 6.0" : "Servlet 6.1+" )+ " <<<<<<");
        int cookieCounter = 0;
        int expectedSetCookieSize = 0;

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestRequestCookieHeader?testName=test_Response_Set_Cookie_Name_Expires_Attribute";
        LOG.info("Sending Request [" + url + "]");
        
        HttpGet getMethod = new HttpGet(url);

        ArrayList<String> expectedCookieList = null;
        
        ArrayList<String> expectedSetCookieHeaders60 = new ArrayList<>();
        expectedSetCookieHeaders60.add("cookieviaMaxAge=cookieValue; Expires="); //Do not include time as setMaxAge is dynamic time when test runs 
        expectedSetCookieHeaders60.add("cookie1_manualSetCookie_Name=cookie1_Value");
        expectedSetCookieHeaders60.add("cookie2_Quote'Name=cookie2_Has_CommaTrailing_Value");
        expectedSetCookieHeaders60.add("cookie2_After_Comma_Name=cookie2_After_Comma_Value; Expires=Sat, 01 Mar 2036");
        expectedSetCookieHeaders60.add("cookie3_Mix_Quotes_InValue_Name=cookie3_Mix_SQuote'And_DQuote\"_Value; Expires=Sat, 01 Mar 2036");
        expectedSetCookieHeaders60.add("cookie4_Mix_SQuotes'_AND_DQuote\"_In_Name=cookie4_Mix_QuotesInName_Value");
        expectedSetCookieHeaders60.add("cookie5_WRAP_DQuote_InValue_Name=cookie5_WRAP_DQuote_Value");
        expectedSetCookieHeaders60.add("cookie6_DQuote_\"_In_Name=cookie6_Value; Expires=Sat, 01 Mar 2036");
        expectedSetCookieHeaders60.add("cookie7_WRAP_DQuote_In_Name=cookie7_Value; Expires=Sat, 01 Mar 2036");

        ArrayList<String> expectedSetCookieHeaders61 = new ArrayList<>();
        expectedSetCookieHeaders61.add("cookieviaMaxAge=cookieValue; Expires=");
        expectedSetCookieHeaders61.add("cookie1_manualSetCookie_Name=cookie1_Value");
        expectedSetCookieHeaders61.add("cookie3_Mix_Quotes_InValue_Name=cookie3_Mix_SQuote'And_DQuote\"_Value; Expires=Sat, 01 Mar 2036");
        expectedSetCookieHeaders61.add("cookie5_WRAP_DQuote_InValue_Name=\"cookie5_WRAP_DQuote_Value\"");
        
        expectedCookieList = ee10 ? (new ArrayList<>(expectedSetCookieHeaders60)) : (new ArrayList<>(expectedSetCookieHeaders61)); 

        expectedSetCookieSize = expectedCookieList.size();

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                Header[] headers = response.getHeaders();
                int matchedCookie = 0;
                for (Header header : headers) {
                    if (header.getName().equals("Set-Cookie")){
                        LOG.info(header.toString());
                        cookieCounter++;

                        if (expectedCookieList.stream().anyMatch(s -> header.getValue().contains(s))){
                            LOG.info("Matched Header : " + header.getValue());
                            matchedCookie++;
                        }
                    }
                }

                LOG.info("Response Set-Cookie: Total: [" + cookieCounter +"] . Expected [" + expectedSetCookieSize + "] . Found [" + matchedCookie + "]");
                assertTrue("Response Set-Cookie: Expected [" + expectedSetCookieSize + "] . Found matched cookies [" + matchedCookie + "]", expectedSetCookieSize == matchedCookie);
            }
        }
    }
    
    /*
     * Test special cases:
     * =noNameWithValue
     * nameWithEmptyValue=""
     * nameWithoutAnyValue=
     * nameOnly
     * endingSemiName=NoPairAfterSemi;
     */
    @Test
    public void test_Cookie_Other() throws Exception {
        LOG.info(">>>>> test_Cookie_Other <<<<<<");

        String testName = "test_Cookie_Other";
        String cookieHeader = "=noNameWithValue; nameWithEmptyValue=\"\"; nameWithoutAnyValue=; nameOnly; endingSemiName=NoPairAfterSemi;";

        sendRequest(testName, cookieHeader);
    }

    /*
     * Servlet 6.0:
     *      Name: Only Wrapped DQuote is valid but quotes are not part of the name; Anywhere else is invalid
     *      Value: Wrapped DQuote is removed. Anywhere else is part of the value.
     *      
     * Servlet 6.1:
     *      Name: No DQuote anywhere in name. SQuote is allowed and part of the name
     *      Value: All quotes are part of value. Wrapped quotes are part of the value
     *          
     */
    @Test
    public void test_Cookie_Quoted_Value() throws Exception {
        LOG.info(">>>>> test_Cookie_Quoted_Value <<<<<<");

        String testName = "test_Cookie_Quoted_Value";
        String cookieHeader = "DQuote\"InName=INVALID; SQuote'In_Name=Keep_SQuote ; Mix_SQuote'And\"_DQuote_In_Name=INVALID; \"WRAP_DQuote_InName\"=Keep_But_Removed_All_DQuote; \"Two_DQuote_AnyWhere\"_InName=INVALID; Mix_Quotes_InValue_Name=Keep_All_DQuote\"And'SQuote_Name; WRAP_SQuote_InValue_Name='Keep_All_SQuote'; WRAP_DQuote_InValue_Name=\"Keep_But_Removed_All_DQuote\"";

        sendRequest(testName, cookieHeader);
    }

    /**
     * Test COOKIE header with All comma delimiter.
     *  "name1=value1, $NAME3=Dollar$Value, Domain=DomainValue"
     *
     * 6.0: All cookie with comma are parsed.
     * 6.1: All Cookie with comma are discard. getCookie() return null
     */
    @Test
    public void test_Cookie_All_Comma_Delimiter() throws Exception {
        LOG.info(">>>>> test_Cookie_All_Comma_Delimiter <<<<<<");

        String testName = "test_Cookie_All_Comma_Delimiter";
        String cookieHeader = "name1=value1, $NAME3=Dollar$Value, Domain=DomainValue";

        sendRequest(testName, cookieHeader);
    }
    
    /**
     * Test COOKIE header with mix comma and semicolon delimiters.
     * Cookie:
     * $Version=1; cookie1_Name=good; cookie2_Name=reject, cookie3_name=reject; middleSemiColonName=good; $NAME2=DollarNameValue, Domain=DomainValue; end1_Name=good; end2_Name=good;
     *
     * 6.0: All cookies are valid
     * 6.1: Any Cookie comma pair are discard.
     */
    @Test
    public void test_Cookie_Mix_Comma_Semicolon_Delimiter() throws Exception {
        LOG.info(">>>>> test_Cookie_Mix_Comma_Semicolon_Delimiter <<<<<<");

        String testName = "test_Cookie_Mix_Comma_Semicolon_Delimiter";
        String cookieHeader = "$Version=1; cookie1_Name=good; cookie2_Name=reject, cookie3_name=reject; middleSemiColonName=good; $NAME2=DollarNameValue, Domain=DomainValue; end1_Name=good; end2_Name=good";

        sendRequest(testName, cookieHeader);
    }

    /*
     * application servlet will verify the cookies and response PASS or FAIL.
     */
    private void sendRequest(String urlPattern, String cookieHeader) throws Exception {
        String EXPECTED_TEXT = "Result [PASS]";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestRequestCookieHeader?testName=" + urlPattern;
        
        /* To test locally against any server (even none-Liberty like tWAS, TC): 
         *    run ./gradlew test once to obtain the generated .war , 
         *    deploy on that test server (mind the javax and jakarta namespace server.)
         *    comment out all the "String url" lines in this test and replacing with url contain "http://localhost:localPort" like below.
         *    run the client ./gradlew test normally.
         * 
         * String url = "http://localhost:9860" +  "/" + TEST_APP_NAME + "/TestRequestCookieHeader?testName=" + urlPattern;
         */


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
