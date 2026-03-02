/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package requestcookieheader.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Request Cookie header according to RFC 6265:
 *
 * 1. Semicolon is the only delimiter. Command is discarded
 * 2. Cookie value can have single and double quotes as part of value
 * 3. Cookie name cannot have double quotes
 *
 * request URL: /Test61RequestCookieHeader
 */
@WebServlet("/Test61RequestCookieHeader")
public class TestRequestCookieHeader extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestRequestCookieHeader.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    private static final String PASS_TEXT = "Result [PASS]";
    private static final String FAIL_TEXT = "Result [FAIL]";

    public TestRequestCookieHeader() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String testName = request.getParameter("testName");

        if (testName == null) {
            return;
        }
        else if (testName.equalsIgnoreCase("test_Cookie_All_Comma_Delimiter")) {
            test_Cookie_All_Comma_Delimiter(request,  response);
        }
        else if (testName.equalsIgnoreCase("test_Cookie_Mix_Comma_Semicolon_Delimiter")) {
            test_Cookie_Mix_Comma_Semicolon_Delimiter(request, response);
        }
        else if (testName.equalsIgnoreCase("test_Cookie_Quoted_Value")) {
            test_Cookie_Quoted_Value(request, response);
        }
        else if (testName.equalsIgnoreCase("test_Response_Set_Cookie_Name_Expires_Attribute")) {
            test_Response_Set_Cookie_Name_Expires_Attribute(request, response);
        }
    }

    /*
     * Request with header:
     * Cookie: $Version=1, commaName1=commaValue1, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue
     *
     * All comma cookie parts are discard; thus cookie is null
     *
     */
    private void test_Cookie_All_Comma_Delimiter(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_All_Comma_Delimiter");
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_All_Comma_Delimiter ======");

        Cookie[] cookies = request.getCookies();
        if (cookies == null){
            LOG.info("No Cookie is found. " + PASS_TEXT);
            sBuilderResponse.append("No Cookie is found. " + PASS_TEXT);
        }
        else {
            LOG.info("Cookie is found but not expected. " + FAIL_TEXT);
            for (Cookie cookie : cookies) {
                //Display anyway for debugging
                LOG.info("Cookie name [" +cookie.getName() + "] , value [" + cookie.getValue() + "]");
            }

            sBuilderResponse.append("Cookie is found but not expected. " + FAIL_TEXT);
        }

        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());
    }

    /*
     * Request Cookie:
     *  $Version=1, comma_Name1=Invalid; middleSemiColonName=middleSemiColonValue, $Path=/Dollar_Path, $Domain=localhost, $NAME2=DollarNameValue, Domain=DomainValue; endSemiColon_Name=endSemiColonValue, comma_Name2=Invalid";
     *
     * Version is not a cookie pair; thus not included in the request.getCookies();
     */
    private void test_Cookie_Mix_Comma_Semicolon_Delimiter(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_Mix_Comma_Semicolon_Delimiter");
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_Mix_Comma_Semicolon_Delimiter ======");

        ArrayList<String> expectedCookieList = new ArrayList<>(Arrays.asList("middleSemiColonName=middleSemiColonValue","endSemiColon_Name=endSemiColonValue"));
        int cookieCounter = 0;
        int expectedNumCookies = expectedCookieList.size();
        String cookiePair = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookiePair = cookie.getName() + "=" + cookie.getValue();
                LOG.info("Cookie pair [" + cookiePair + "]");

                expectedCookieList.remove(cookiePair);
                cookieCounter++;
            }
        } else {
            LOG.info("No cookies found");
        }

        LOG.info("Expected pairs [" + expectedNumCookies+"] ; found [" + cookieCounter + "] . Remaining [" + expectedCookieList.size() + "] in cookie list; Expecting 0" );

        if (expectedCookieList.size() > 0) {
            LOG.info("Remaining item :");
            for (String item : expectedCookieList) {
                LOG.info(item);
            }
        }

        if (cookieCounter != expectedNumCookies || expectedCookieList.size() != 0) {
            String message = "Cookie pairs NOT match: Expecting [" + expectedNumCookies + "] but found [" + cookieCounter + "]. Or Cookie list remaining [" + expectedCookieList.size() + "] but expecting 0";
            LOG.info(message + " " + FAIL_TEXT);
            sBuilderResponse.append(message + FAIL_TEXT);
        }
        else {
            LOG.info(PASS_TEXT);
            sBuilderResponse.append(PASS_TEXT);
        }
        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());
    }

    /**
     * Test Request Cookie header with quote
     * "$Version=1; badDouble\"InName=InValid, comma_Name=Double\"InValue_Invalid; test_SingleQuote'In_Name=Valid; \"badDouble2_InName\"=InValid; test_MixQuotes_InValue_Name=\"Mix_Double'And'Single_Quotes_Name_Valid\"; test_SingleQuote_InValue_Name='Single_Quote_Value_Valid'; test_NoQuote_Name=NoQuoteValue_Valid; test_2DoubleQuote_InValue_Name=\"DoubleInValue_Valid\"; badDouble\"ENDInName=InValid";
     *
     * Cookie and Set-Cookie Name:
     *  No - Double quote
     *  Yes - Single quote
     *
     * Cookie Value:
     *  Yes - Double and Single
     */
    private void test_Cookie_Quoted_Value(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_Quoted_Value");
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_Quoted_Value ======");
        ArrayList<String> expectedCookieList = new ArrayList<>();

        expectedCookieList.add("test_SingleQuote'In_Name=Valid");
        expectedCookieList.add("test_MixQuotes_InValue_Name=\"Mix_Double'And'Single_Quotes_Name_Valid\"");
        expectedCookieList.add("test_SingleQuote_InValue_Name='Single_Quote_Value_Valid'");
        expectedCookieList.add("test_NoQuote_Name=NoQuoteValue_Valid");
        expectedCookieList.add("test_2DoubleQuote_InValue_Name=\"DoubleInValue_Valid\"");

        int cookieCounter = 0;
        int expectedNumCookies = expectedCookieList.size();
        String cookiePair = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookiePair = cookie.getName() + "=" + cookie.getValue();
                LOG.info("Cookie pair [" + cookiePair + "]");

                expectedCookieList.remove(cookiePair);
                cookieCounter++;
            }
        } else {
            LOG.info("No cookies found. TEST FAIL");
        }

        LOG.info("Expected pairs [" + expectedNumCookies+"] ; found [" + cookieCounter + "] . Remaining [" + expectedCookieList.size() + "] in cookie list; Expecting 0" );

        if (expectedCookieList.size() > 0) {
            LOG.info("Remaining item :");
            for (String item : expectedCookieList) {
                LOG.info(item);
            }
        }

        if (cookieCounter != expectedNumCookies || expectedCookieList.size() != 0) {
            String message = "Cookie pairs NOT match: Expecting [" + expectedNumCookies + "] but found [" + cookieCounter + "]. Or Cookie list remaining [" + expectedCookieList.size() + "] but expecting 0";
            LOG.info(message + " " + FAIL_TEXT);
            sBuilderResponse.append(message + FAIL_TEXT);
        }
        else {
            LOG.info(PASS_TEXT);
            sBuilderResponse.append(PASS_TEXT);
        }
        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());
    }

    /*
     * Response Set-Cookie:
     *  Test quotes in name and value
     *  Test comma ends pair (which is discarded)
     *  Test comma is kept in Expires attribute
     */
    private void test_Response_Set_Cookie_Name_Expires_Attribute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Response_Set_Cookie_Name_Expires_Attribute");

        ServletOutputStream sos = response.getOutputStream();
        sos.println("Response from TestRequestCookieHeader.test_Request_Set_Cookie_Name_Expires_Attribute");

        Cookie testCookie = new Cookie("cookieviaMaxAge", "cookieValue");
        testCookie.setMaxAge(3600);     //will add ; Expires attribute
        response.addCookie(testCookie);

        String manualSetCookie = "manualSetCookie_Valid_Name=cookieValue; Expires=Sat, 01 Mar 2036 19:00:00 GMT; HttpOnly";
        response.addHeader("Set-Cookie", manualSetCookie);

        // , cookie2_Comma_Pair=Invalid is discarded
        String manualSetCookie2 = "cookie2_Quote'Name=Value_Valid, cookie2_Comma_Pair=Invalid; Expires=Sat, 01 Mar 2036 19:00:00 GMT; HttpOnly";
        response.addHeader("Set-Cookie", manualSetCookie2);

        //mix quotes in Value
        String manualSetCookie3 = "cookie3_Quotes_InValue_Name=Quote's\"_Valid";
        response.addHeader("Set-Cookie", manualSetCookie3);

        //double quoted value
        String manualSetCookie4= "cookie4_2Quotes_InValue_Name=\"Quote_Valid\"";
        response.addHeader("Set-Cookie", manualSetCookie4);

        //single double in name
        String manualSetCookie_InValid_1 = "manualSetCookie_InValid_1\"_Name=InValid; Expires=Sat, 01 Mar 2036 19:00:00 GMT";
        response.addHeader("Set-Cookie", manualSetCookie_InValid_1);

        //2 double quotes in name
        String manualSetCookie_InValid_2 = "\"manualSetCookie_InValid_2\"_Name=InValid; Expires=Sat, 01 Mar 2036 19:00:00 GMT";
        response.addHeader("Set-Cookie", manualSetCookie_InValid_2);
    }
}
