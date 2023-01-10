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
package requestcookieheader.servlets;

import java.io.IOException;
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

    public TestRequestCookieHeader() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String testName = request.getParameter("testName");

        if (testName == null) {
            return;
        }
        else if (testName.equalsIgnoreCase("MixCookieNames")) {
            testMixCookieNamesWithDollarSign(request,  response);
        }
        else if (testName.equalsIgnoreCase("MaxAgeZero")) {
            testMaxAgeZero(request, response); 
        }
    }

    /*
     * Request with header:
     * Cookie: $Version=1; name1=value1; $Path=/Dollar_Path; $Domain=localhost; $NAME2=DollarNameValue; Domain=DomainValue
     * Expecting 5 cookies
     */
    private void testMixCookieNamesWithDollarSign(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
    
    private void testMaxAgeZero(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("Test testMaxAgeZero");

        ServletOutputStream sos = response.getOutputStream();
        sos.println("Hello World from TestRequestCookieHeader.testMaxAgeZero");
        
        Cookie testCookie = new Cookie("cookieName", "cookieValue");
        testCookie.setVersion(0);

        testCookie.setMaxAge(0);
        response.addCookie(testCookie);
        
        
        
    }

}
