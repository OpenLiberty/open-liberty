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
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Request Cookie header according to RFC 6265:
 * Semicolon is the only delimiter. Command is discarded
 *
 *  (For test quoted name and value, see bucket 6.0 tests)
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
}
