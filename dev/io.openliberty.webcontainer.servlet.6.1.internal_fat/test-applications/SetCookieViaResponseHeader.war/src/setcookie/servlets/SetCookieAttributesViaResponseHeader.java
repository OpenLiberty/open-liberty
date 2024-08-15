/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package setcookie.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Set-Cookie servlet using Cookie API, reponse setHeader() and addHeader() to verify that the Set-Cookie is not split for arbitrary attributes
 * Also verify: Attribute name with EMPTY value does not have trailing equal sign
 *              Attribute name with NULL value removes the existing attribute name and itself.
 */
@WebServlet(urlPatterns = {"/TestSetCookieAttributesViaResponseHeader/*"}, name = "SetCookieAttributesViaResponseHeader")
public class SetCookieAttributesViaResponseHeader extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = SetCookieAttributesViaResponseHeader.class.getName();

    //Common
    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;

    final String SET_COOKIE = "Set-Cookie";

    final String SET_COOKIE_VALUE = "SET_COOKIE_ATT_TEST=SET_COOKIE_VALUE; Secure; SameSite=None; SET_ATT_NAME=SET_ATT_VALUE; HttpOnly";
    final String ADD_COOKIE_VALUE = "ADD_COOKIE_ATT_TEST=ADD_COOKIE_VALUE; ADD_ATT_NAME=ADD_ATT_VALUE; Secure; SameSite=Strict; HttpOnly";

    final String SET_COOKIE_EMPTY_VALUE = "SET_COOKIE_EMPTY_TEST=SET_COOKIE_VALUE; SET_ATT_NAME=SET_ATT_VALUE; Secure; SameSite=None; SET_NAME_ONLY_SIGN=; SET_NAME_ONLY";
    final String ADD_COOKIE_EMPTY_VALUE = "ADD_COOKIE_EMPTY_TEST=ADD_COOKIE_VALUE; Secure; ADD_NAME_ONLY_SIGN=; ADD_NAME_ONLY; ADD_ATT_NAME=ADD_ATT_VALUE; SameSite=Strict";

    final String SET_COOKIE_NULL_VALUE = "SET_COOKIE_NULL_TEST=SET_COOKIE_VALUE; SET_ATT_NAME=SET_ATT_VALUE; Secure; SameSite=None; SET_BE_REMOVED=REMOVE_ME; SET_BE_REMOVED=null; SET_NULL_VALUE=null";
    final String ADD_COOKIE_NULL_VALUE = "ADD_COOKIE_NULL_TEST=ADD_COOKIE_VALUE; ADD_ATT_NAME=ADD_ATT_VALUE; ADD_BE_REMOVED=REMOVE_ME; Secure; ADD_BE_REMOVED=null; ADD_NULL_VALUE=null; SameSite=Strict";

    final String SET_COOKIE_ILLEGAL_NAME = "SET_COOKIE_ILLEGAL_NAME_TEST=SET_COOKIE_VALUE; SET_ATT_NAME=SET_ATT_VALUE; Secure; SameSite=None; SET_?_QUESTION_MARK_IS_ILLEGAL=SET_NAME; SET_NULL_VALUE=null";


    public SetCookieAttributesViaResponseHeader() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGENTER("doGET");

        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        switch (request.getHeader("runTest")) {
            case "test_SetCookie_Attributes" : test_SetCookie_Attributes(); break;
            case "test_SetCookie_Attributes_EMPTY" : test_SetCookie_Attributes_EMPTY(); break;
            case "test_SetCookie_Attributes_NULL" : test_SetCookie_Attributes_NULL(); break;
            case "test_SetCookie_Illegal_Attribute_Name" : test_SetCookie_Illegal_Attribute_Name(); break;
            case "test_RequestCookie_QuotedValue" : test_RequestCookie_QuotedValue(); break;
        }

        if (!responseSB.isEmpty()) {
            LOG("Response Text [" + responseSB.toString() + "]");         //Look for this log even there is no enabled trace
            sos.println(responseSB.toString());
        }

        LOGEXIT("doGET");
    }

    /*
     * Create Set-Cookie via Cookie(), response.setHeader, and response.setHeader
     * Set arbitrary attribute
     */
    private void test_SetCookie_Attributes() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        LOG1("Set-Cookie using Cookie API");
        Cookie wcCookieAtt = new Cookie("Cookie_ATT_Test", "Cookie_Value");
        wcCookieAtt.setAttribute("Cookie_ATT_Name", "Cookie_ATT_Value");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setSecure(false);

        response.addCookie(wcCookieAtt);

        LOG1("setHeader Set-Cookie");
        response.setHeader(SET_COOKIE,SET_COOKIE_VALUE);

        LOG1("addHeader Set-Cookie");
        response.addHeader(SET_COOKIE,ADD_COOKIE_VALUE);

        responseSB.append("Test Set-Cookie attribute name and attribute value using Cookie() API, response.setHeader() and addHeader()");

        LOGEXIT(method);
    }

    /*
     * To test attribute name with empty value.
     * setAttribute ("name","") -> ; name
     */
    private void test_SetCookie_Attributes_EMPTY() throws IOException{

        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        LOG1("Create Set-Cookie using Cookie API");
        Cookie wcCookieAtt = new Cookie("Cookie_EMPTY_TEST", "Cookie_Value");
        wcCookieAtt.setAttribute("Cookie_ATT_Name", "Cookie_ATT_Value");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setAttribute("Cookie_NAME_ONLY", "");
        wcCookieAtt.setAttribute("Cookie_NAME_ONLY_SIGN", "=");         //value is "=" which should be removed

        response.addCookie(wcCookieAtt);

        LOG1("setHeader Set-Cookie");
        response.setHeader(SET_COOKIE,SET_COOKIE_EMPTY_VALUE);

        LOG1("addHeader Set-Cookie");
        response.addHeader(SET_COOKIE,ADD_COOKIE_EMPTY_VALUE);

        responseSB.append("Test Set-Cookie with EMPTY attribute values using Cookie API, response.setHeader and addHeader");

        LOGEXIT(method);
    }

    /*
     * To test NULL attribute value which remove existing att name, also itself
     */
    private void test_SetCookie_Attributes_NULL() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();

        LOGENTER(method);

        LOG1("Create Set-Cookie using Cookie API");
        Cookie wcCookieAtt = new Cookie("Cookie_NULL_TEST", "Cookie_Value");
        wcCookieAtt.setAttribute("Cookie_ATT_Name", "Cookie_ATT_Value");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setAttribute("Cookie_NULL_VALUE", null);
        wcCookieAtt.setAttribute("Cookie_BE_REMOVED", "REMOVE_ME");
        wcCookieAtt.setAttribute("Cookie_BE_REMOVED", null);

        response.addCookie(wcCookieAtt);

        LOG1("setHeader Set-Cookie with NULL attribute value");
        response.setHeader(SET_COOKIE,SET_COOKIE_NULL_VALUE);

        LOG1("addHeader Set-Cookie with NULL attribute value");
        response.addHeader(SET_COOKIE,ADD_COOKIE_NULL_VALUE);

        responseSB.append("Test Set-Cookie with NULL attribute values using Cookie API, response.setHeader and addHeader");

        LOGEXIT(method);
    }

    /*
     * Attribute name cannot have special characters "()<>@,;:\\/[]?={} \t
     *
     * Runtime IllegalArgumentException with translated message "CWWKT0047E: Cookie attribute name [{0}] contains an invalid character."
     * The bad set-cookie is dropped and Response continues.
     *
     */
    private void test_SetCookie_Illegal_Attribute_Name() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        LOG1("setHeader Set-Cookie with Illegal character in attribute name.  This will result in exception!");
        response.setHeader(SET_COOKIE,SET_COOKIE_ILLEGAL_NAME);

        LOG1("Create Set-Cookie using Cookie API");
        Cookie wcCookieAtt = new Cookie("Cookie_ATT_NAME_TEST", "Cookie_Value");
        wcCookieAtt.setAttribute("Cookie_ATT_Name", "Cookie_ATT_Value");
        wcCookieAtt.setHttpOnly(true);

        response.addCookie(wcCookieAtt);

        LOGEXIT(method);
    }

    /*
     * value with escape quote should retain after parsing
     * "Cookie: name1=\"value1\";
     *  > getName = name1
     *  > getValue = "value1"  (quotes are part of the value)
     *  
     *  Client will assert the response text
     */
    private void test_RequestCookie_QuotedValue() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        Cookie[] cookiesArray = request.getCookies();
        for (Cookie c : cookiesArray) {
            responseSB.append("Cookie name [" + c.getName() + "] , value [" + c.getValue() + "]\n");
        }

        LOGEXIT(method);
    }

    //#########################################################
    private void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }

    //one tab \t
    private void LOG1(String s) {
        System.out.println(CLASS_NAME + "\t" + s);
    }

    private void LOGENTER(String method) {
        LOG(">>>>>>>>>>>>>>>> TESTING [" + method + "] ENTER >>>>>>>>>>>>>>>>");
    }

    private void LOGEXIT(String method) {
        LOG("<<<<<<<<<<<<<<<<<< TESTING [" + method + "] EXIT <<<<<<<<<<<<<<<<<<");
    }
}
