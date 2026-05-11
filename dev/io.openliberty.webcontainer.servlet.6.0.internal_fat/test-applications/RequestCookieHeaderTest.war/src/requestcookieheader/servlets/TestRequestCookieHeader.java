/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Request Cookie header according to RFC 6265:
 * 1. Except $version, $ prefix any name will be part of the new cookie name (including $ sign).
 *      That also applies to those special attributes like Domain, Path
 * 2. Max-Age - if set to 0, it should include in the Set-Cookie 
 * 
 * request URL: /TestRequestCookieHeader
 */
@WebServlet("/TestRequestCookieHeader")
public class TestRequestCookieHeader extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestRequestCookieHeader.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    final String PASS_TEXT = "Result [PASS]";
    final String FAIL_TEXT = "Result [FAIL]";

    String servletVersion;

    public TestRequestCookieHeader() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String testName = request.getParameter("testName");

        ServletContext servletContext = request.getServletContext();
        servletVersion = servletContext.getMajorVersion() + "." + servletContext.getMinorVersion();

        LOG.info("Version: [" + servletVersion + "]");

        if (testName == null) {
            return;
        } else if (testName.equalsIgnoreCase("test_Mix_Cookie_Name_With_Dollar_Signs")) {
            test_MixCookieNameWithDollarSigns(request, response);
        } else if (testName.equalsIgnoreCase("test_MaxAgeZero")) {
            test_MaxAgeZero(request, response);
        } else if (testName.equalsIgnoreCase("test_Cookie_Other")) {
            test_Cookie_Other(request, response);
        } else if (testName.equalsIgnoreCase("test_Cookie_Quoted_Value")) {
            test_Cookie_Quoted_Value(request, response);
        } else if (testName.equalsIgnoreCase("test_Response_Set_Cookie_Name_Expires_Attribute")) {
            test_Response_Set_Cookie_Name_Expires_Attribute(request, response);
        } else if (testName.equalsIgnoreCase("test_Cookie_Mix_Comma_Semicolon_Delimiter")) {
            test_Cookie_Mix_Comma_Semicolon_Delimiter(request, response);
        } else if (testName.equalsIgnoreCase("test_Cookie_All_Comma_Delimiter")) {
            test_Cookie_All_Comma_Delimiter(request, response);
        }
    }

    /*
     * Request with header:
     * Cookie: $Version=1; name1=value1; $Path=/Dollar_Path; $Domain=localhost; $NAME2=DollarNameValue; Domain=DomainValue
     * Expecting 5 cookies
     */
    private void test_MixCookieNameWithDollarSigns(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test testMixCookieNamesWithDollarSign");
        Cookie[] cookie = request.getCookies();
        int i = cookie.length;
        boolean testPass = true;
        StringBuilder sBuilderResponse = new StringBuilder("TEST testMixDollarSigns . Message [[[--");
        
        //Test number of found cookies
        if (i != 5) {
            testPass = false;
            sBuilderResponse.append(" FAIL test number of cookies, expecting 5 cookies , found [" + i + "] . Test is not complete.  Check the test case and trace |");
            LOG.info("Test number of cookies FAIL");

            sBuilderResponse.append(" --]]] Result [FAIL]");
            response.setHeader("TestResult", sBuilderResponse.toString());  
            
            return; //do not continue as it will mess up later tests index
        }
        else
            sBuilderResponse.append(" test number of cookies, found ["+i+"] cookies. PASS |");

        //Cookies should be in order showing in the request header

        //1st cookie, expecting cookie name1=value1
        if (!(cookie[0].toString().contains("name1=value1"))) {
            testPass = false;
            sBuilderResponse.append(" FAIL , expecting [name1=value1] cookie. Actual [" + cookie[0] + "] |");
            LOG.info("Test [name1=value1] cookie FAIL");
        }
        else 
            sBuilderResponse.append(" Test [name1=value1] cookie  PASS |");
        
        //2nd cookie, expecting cookie $Path=/Dollar_Path
        if (!(cookie[1].toString().contains("$Path=/Dollar_Path"))) {
            testPass = false;
            sBuilderResponse.append(" FAIL , expecting [$Path=/Dollar_Path] cookie. Actual [" + cookie[1] + "] |");
            LOG.info("Test [name1=value1] cookie FAIL");
        }
        else 
            sBuilderResponse.append(" Test [$Path=/Dollar_Path] cookie  PASS |");

        //3rd, expecting cookie $Domain=localhost
        if (!(cookie[2].toString().contains("$Domain=localhost"))) {
            testPass = false;
            sBuilderResponse.append(" FAIL , expecting [$Domain=localhost] cookie. Actual [" + cookie[2] + "] |");
            LOG.info("Test [name1=value1] cookie FAIL");
        }
        else 
            sBuilderResponse.append(" Test [$Domain=localhost] cookie  PASS |");
        
        //4th, expecting cookie $NAME2=DollarNameValue"
        if (!(cookie[3].toString().contains("$NAME2=DollarNameValue"))) {
            testPass = false;
            sBuilderResponse.append(" FAIL , expecting [$NAME2=DollarNameValue] cookie. Actual [" + cookie[3] + "] |");
            LOG.info("Test [name1=value1] cookie FAIL");
        }
        else 
            sBuilderResponse.append(" Test [$NAME2=DollarNameValue] cookie  PASS |");
        
        //5th, expecting cookie Domain=DomainValue
        if (!(cookie[4].toString().contains("Domain=DomainValue"))) {
            testPass = false;
            sBuilderResponse.append(" FAIL , expecting [Domain=DomainValue] cookie. Actual [" + cookie[4] + "] |");
            LOG.info("Test [name1=value1] cookie FAIL");
        }
        else 
            sBuilderResponse.append(" Test [Domain=DomainValue] cookie  PASS |");


        //Final result - any of the above tests fail will make test fail
        if (testPass)
            sBuilderResponse.append(" --]]] Result [PASS]");
        else
            sBuilderResponse.append(" --]]] Result [FAIL]");
       
       
        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());   
    }

    private void test_MaxAgeZero(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test testMaxAgeZero");

        ServletOutputStream sos = response.getOutputStream();
        sos.println("Hello World from TestRequestCookieHeader.testMaxAgeZero");
        
        Cookie testCookie = new Cookie("cookieName", "cookieValue");
        testCookie.setVersion(0);

        testCookie.setMaxAge(0);
        response.addCookie(testCookie);
    }

    /*
     * Special cases:
     * Cookie "=noNameWithValue; nameWithEmptyValue=\"\"; nameWithoutAnyValue=; nameOnly; endingSemiName=NoPairAfterSemi;"
     */
    private void test_Cookie_Other(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_Other. Servlet Version " + servletVersion);
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_Other ======");
        boolean testFail = false;
        String name = null;
        String value = null;
        int counter = 0;

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            LOG.info("No Cookie is found. " + FAIL_TEXT);
            sBuilderResponse.append("No Cookie is found. " + FAIL_TEXT);
        } else {
            LOG.info("Cookies : ");
            for (Cookie cookie : cookies) {
                //Display anyway for debugging
                LOG.info("Cookie name[" + (name = cookie.getName()) + "] , value [" + (value = cookie.getValue()) + "]");

                if (name.equalsIgnoreCase("nameWithEmptyValue")) {
                    if (servletVersion.equals("6.0")) {                   // 6.0 strips off WRAP DQuotes
                        testFail = ((value.isEmpty()) ? false : true);
                    } else {
                        testFail = (value.equals("\"\"") ? false : true); // 6.1 Quotes are part of value
                    }
                } else if (name.equalsIgnoreCase("nameWithoutAnyValue")) {
                    testFail = ((value.isEmpty()) ? false : true);
                } else if (name.equalsIgnoreCase("nameOnly")) {
                    testFail = ((value.isEmpty()) ? false : true);
                } else if (name.equalsIgnoreCase("endingSemiName")) {
                    testFail = ((value.equalsIgnoreCase("NoPairAfterSemi")) ? false : true);
                }

                counter++;

                if (testFail) {
                    String message = "Test Cookie name [" + name + "] , value [" + value + "] " + FAIL_TEXT;
                    LOG.info(message);
                    sBuilderResponse.append(message);
                    break;
                }
            }

            if (counter != 4) {
                sBuilderResponse.append("Expecting 4 Cookies ; but found [" + counter + "] " + FAIL_TEXT);
            } else {
                LOG.info(PASS_TEXT);
                sBuilderResponse.append(PASS_TEXT);
            }
        }

        response.setHeader("TestResult", sBuilderResponse.toString());
    }

    /*
     * Request Cookie with quotes:
     * 
     * "DQuote\"InName=INVALID; SQuote'In_Name=Keep_SQuote ; Mix_SQuote'And\"_DQuote_In_Name=INVALID; \"WRAP_DQuote_InName\"=Keep_But_Removed_All_DQuote; \"Two_DQuote_AnyWhere\"_InName=INVALID; Mix_Quotes_InValue=Keep_All_DQuote\"And'SQuote_Name; WRAP_SQuote_InValue='Keep_All_SQuote'; WRAP_DQuote_InValue=\"Keep_But_Removed_All_DQuote\"";
     */
    private void test_Cookie_Quoted_Value(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_Quoted_Value. Servlet Version " + servletVersion);
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_Quoted_Value ======");
        
        int cookieCounter = 0;
        String cookiePair = null;
        ArrayList<String> expectedCookieList = null;

        /*
         * Servlet 6.0:
         *      Name: Only Wrapped DQuote is valid but remove from the name; Anywhere else is invalid
         *      Value: DQuote can be anywhere and is part of value. Wrapped DQuotes are removed
         */
        ArrayList<String> expectedCookieListServlet60 = new ArrayList<>();
        expectedCookieListServlet60.add("SQuote'InName=Keep_SQuote");
        expectedCookieListServlet60.add("WRAP_DQuote_InName=Keep_But_Removed_All_DQuote"); 
        expectedCookieListServlet60.add("Mix_Quotes_InValue=Keep_All_DQuote\"And'SQuote_Name");
        expectedCookieListServlet60.add("WRAP_SQuote_InValue='Keep_All_SQuote'");
        expectedCookieListServlet60.add("WRAP_DQuote_InValue=Keep_But_Removed_All_DQuote");
       
        /*
         * Servlet 6.1:
         *      Value: Any quote (anywhere) is part of value. Wrapped quotes are part of the value
         */
        ArrayList<String> expectedCookieListServlet61 = new ArrayList<>();
        expectedCookieListServlet61.add("SQuote'InName=Keep_SQuote");
        expectedCookieListServlet61.add("WRAP_DQuote_InName=Keep_But_Removed_All_DQuote");      //revisit in servlet-next 
        expectedCookieListServlet61.add("Mix_Quotes_InValue=Keep_All_DQuote\"And'SQuote_Name");
        expectedCookieListServlet61.add("WRAP_SQuote_InValue='Keep_All_SQuote'");
        expectedCookieListServlet61.add("WRAP_DQuote_InValue=\"Keep_With_All_Wrapped_DQuote\"");  //Wrapped DQuotes are part of value

        expectedCookieList = servletVersion.equals("6.0") ? (new ArrayList<>(expectedCookieListServlet60)) : (new ArrayList<>(expectedCookieListServlet61)); 

        int expectedNumCookies = expectedCookieList.size();

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

        LOG.info("Expected pairs [" + expectedNumCookies + "] ; found [" + cookieCounter + "] . Remaining [" + expectedCookieList.size() + "] in cookie list but Expecting 0");

        if (expectedCookieList.size() > 0) {
            LOG.info("Remaining item :");
            for (String item : expectedCookieList) {
                LOG.info(item);
            }
        }

        if (cookieCounter != expectedNumCookies || expectedCookieList.size() != 0) {
            String message = "Cookie pairs NOT match: Expecting [" + expectedNumCookies + "] but found [" + cookieCounter + "]. Or Cookie list remaining ["
                             + expectedCookieList.size() + "] but expecting 0";
            LOG.info(message + " " + FAIL_TEXT);
            sBuilderResponse.append(message + FAIL_TEXT);
        } else {
            LOG.info(PASS_TEXT);
            sBuilderResponse.append(PASS_TEXT);
        }
        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());
    }
    
    /*
     * Response Set-Cookie:
     *  Test quotes in name and value
     *  Test comma pair
     *  Test comma is kept in Expires attribute
     *  
     *  Servlet 6.0: Only remove Wrapped DQuotes.
     *     Expects 9 Set-Cookie headers
     *  
     *  Servlet 6.1: No DQuotes anywhere in Name. Quotes are parts of Value
     *     Expect  5 Set-Cookie headers
     */
    private void test_Response_Set_Cookie_Name_Expires_Attribute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Response_Set_Cookie_Name_Expires_Attribute");

        ServletOutputStream sos = response.getOutputStream();
        sos.println("Response from TestRequestCookieHeader.test_Request_Set_Cookie_Name_Expires_Attribute");

        //Valid in 6.0 and 6.1
        Cookie testCookie = new Cookie("cookieviaMaxAge", "cookieValue");
        testCookie.setMaxAge(3600);     //will add ; Expires attribute
        response.addCookie(testCookie);

        //Valid in 6.0 and 6.1
        String manualSetCookie1 = "cookie1_manualSetCookie_Name=cookie1_Value";
        response.addHeader("Set-Cookie", manualSetCookie1);

        /*
         * cookie2:
         * 
         * 6.0: generate TWO cookie2 set (since comma is also delimiter)
         * (1) cookie2_Quote'Name=cookie2_Has_CommaTrailing_Value
         * (2) cookie2_After_Comma_Name=cookie2_After_Comma_Value; Expires=Sat, 01 Mar 2036 19:00:00 GMT; HttpOnly
         * 
         * 6.1: ignore entire cookie since comma is before first semicolon
         */
        String manualSetCookie2 = "cookie2_Quote'Name=cookie2_Has_CommaTrailing_Value, cookie2_After_Comma_Name=cookie2_After_Comma_Value; Expires=Sat, 01 Mar 2036 19:00:00 GMT; HttpOnly";
        response.addHeader("Set-Cookie", manualSetCookie2);

        //Valid in 6.0 and 6.1 (middle quotes are retained)
        String manualSetCookie3 = "cookie3_Mix_Quotes_InValue_Name=cookie3_Mix_SQuote'And_DQuote\"_Value; Expires=Sat, 01 Mar 2036 19:00:00 GMT";
        response.addHeader("Set-Cookie", manualSetCookie3);

        //mix quotes in Name. Valid in 6.0 (quotes are retained) ; 6.1 discard this cookie
        String manualSetCookie4 = "cookie4_Mix_SQuotes'_AND_DQuote\"_In_Name=cookie4_Mix_QuotesInName_Value";
        response.addHeader("Set-Cookie", manualSetCookie4);

        //wrap value in double quotes. 6.0 retains but removes wrapped quotes; // 6.1 retains wrapped quotes
        String manualSetCookie5= "cookie5_WRAP_DQuote_InValue_Name=\"cookie5_WRAP_DQuote_Value\"";
        response.addHeader("Set-Cookie", manualSetCookie5);

        //single DQuote in name
        // 6.0 including DQuote ; 6.1 discard this cookie as no DQuote anywhere in name
        String manualSetCookie6 = "cookie6_DQuote_\"_In_Name=cookie6_Value ; Expires=Sat, 01 Mar 2036 19:00:00 GMT";
        response.addHeader("Set-Cookie", manualSetCookie6);

        //WRAP name in DQuote
        // 6.0 keeps but removes wrapped DQuote in name ; 6.1 discards this cookie
        String manualSetCookie7 = "\"cookie7_WRAP_DQuote_In_Name\"=cookie7_Value; Expires=Sat, 01 Mar 2036 19:00:00 GMT";
        response.addHeader("Set-Cookie", manualSetCookie7);
    }
    

    /*
     * Request Cookie:
     *  $Version=1; cookie1_Name=good; cookie2_Name=reject, cookie3_name=reject; middleSemiColonName=good; $NAME2=DollarNameValue, Domain=DomainValue; end1_Name=good; end2_Name=good;
     *
     * Version is not a cookie pair; thus not included in the request.getCookies();
     */
    private void test_Cookie_Mix_Comma_Semicolon_Delimiter(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_Mix_Comma_Semicolon_Delimiter. Servlet Version: " + servletVersion);
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_Mix_Comma_Semicolon_Delimiter ======");
        ArrayList<String> expectedCookieList = null;
        
        ArrayList<String> expectedCookieListServlet60 = new ArrayList<>();
        expectedCookieListServlet60.add("cookie1_Name=good");
        expectedCookieListServlet60.add("cookie2_Name=reject");
        expectedCookieListServlet60.add("cookie3_name=reject");
        expectedCookieListServlet60.add("middleSemiColonName=good");
        expectedCookieListServlet60.add("$NAME2=DollarNameValue");
        expectedCookieListServlet60.add("Domain=DomainValue");
        expectedCookieListServlet60.add("end1_Name=good");
        expectedCookieListServlet60.add("end2_Name=good");
        
        expectedCookieList = new ArrayList<>(expectedCookieListServlet60); 


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

        LOG.info("Expected pairs [" + expectedNumCookies+"] ; found [" + cookieCounter + "] . Remaining [" + expectedCookieList.size() + "] in cookie list but Expecting 0" );

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
     * Test all comma delimiter
     * 
     * "name1=value1, $NAME3=Dollar$Value, Domain=DomainValue"
     * 
     * 6.0: all valid cookies
     */
    private void test_Cookie_All_Comma_Delimiter(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info(">>>>> Test test_Cookie_All_Comma_Delimiter . Servlet Version " + servletVersion);
        StringBuilder sBuilderResponse = new StringBuilder("====== TEST test_Cookie_All_Comma_Delimiter ======");

        ArrayList<String> expectedCookieListServlet60 = new ArrayList<>();
        expectedCookieListServlet60.add("name1=value1");
        expectedCookieListServlet60.add("$NAME3=Dollar$Value");
        expectedCookieListServlet60.add("Domain=DomainValue");

        int cookieCounter = 0;
        int expectedNumCookies = expectedCookieListServlet60.size();
        String cookiePair = null;

        Cookie[] cookies = request.getCookies();

        if (cookies == null){
            LOG.info("No Cookie is found. " + FAIL_TEXT);
            sBuilderResponse.append("No Cookie is found. " + FAIL_TEXT);
        }
        else {
            for (Cookie cookie : cookies) {
                cookiePair = cookie.getName() + "=" + cookie.getValue();
                LOG.info("Cookie pair [" + cookiePair + "]");

                expectedCookieListServlet60.remove(cookiePair);
                cookieCounter++;
            }

            String message = "Expected pairs [" + expectedNumCookies+"] ; found [" + cookieCounter + "] . Remaining [" + expectedCookieListServlet60.size() + "] in cookie list but Expecting 0";

            if (expectedCookieListServlet60.size() > 0) {
                LOG.info("Remaining item :");
                for (String item : expectedCookieListServlet60) {
                    LOG.info(item);
                }
            }

            if (cookieCounter != expectedNumCookies || expectedCookieListServlet60.size() != 0) {
                LOG.info(message + " " + FAIL_TEXT);
                sBuilderResponse.append(message + FAIL_TEXT);
            }
            else {
                LOG.info(message + " " + PASS_TEXT);
                sBuilderResponse.append(PASS_TEXT);
            }
        }

        //Client check this header.
        response.setHeader("TestResult", sBuilderResponse.toString());
    }
}
