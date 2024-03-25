/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package headers.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test response add/setHeader, add/setDateHeader, add/setIntHeader
 * Test request getDateHeader and getInHeader : return the first value when there are multiple headers with same name.
 */
@WebServlet("/TestResponseHeaders")
public class TestAddAndSetHeadersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestAddAndSetHeadersServlet.class.getName();

    HttpServletRequest request;
    HttpServletResponse response;
    ServletOutputStream sos;

    public TestAddAndSetHeadersServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        request = req;
        response = resp;
        sos = response.getOutputStream();

        LOG("ENTER doGet");
        sos.println("Hello from the " + CLASS_NAME);

        String runTestMethod = request.getHeader("runTest");
        if (runTestMethod == null) {
            LOG("Run default testAddHeader");
            runTestMethod = "testAddHeader";
        }

        if (runTestMethod.equalsIgnoreCase("testAddHeader"))
            testAddHeader();
        else if (runTestMethod.equalsIgnoreCase("testSetHeader"))
            testSetHeader();
        else if (runTestMethod.equalsIgnoreCase("testAddandSetDateHeader"))
            testAddandSetDateHeader();
        else if (runTestMethod.equalsIgnoreCase("testAddandSetIntHeader"))
            testAddandSetIntHeader();
        else if (runTestMethod.equalsIgnoreCase("testRequestGetDateAndIntHeaders"))
            testRequestGetDateAndIntHeaders();

        LOG("EXIT doGet");
    }

    /**
     * no effect if {@code null} is passed for either the {@code name} or {@code value} parameters.
     *
     * <br>
     * Client to verify response headers: <br>
     * TEST-ADD-HEADER: ONE_addHeaderValue <br>
     * TEST-ADD-HEADER: TWO_addHeaderValue <br>
     * TEST-ADD-HEADER_EMPTY-VALUE: <br>
     */
    private void testAddHeader() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        //Expect 2 headers TEST-ADD-HEADER with different values
        response.addHeader("TEST-ADD-HEADER", "ONE_addHeaderValue");
        response.addHeader("TEST-ADD-HEADER", "TWO_addHeaderValue");

        //Add a null header value; Expect: no change and no exception
        response.addHeader("TEST-ADD-HEADER", null);

        //Add a null name; Expect: no change and no exception
        response.addHeader(null, "addHeaderNameIsNull");

        //Add a null name and null value; Expect: no change and no exception
        response.addHeader(null, null);

        //Add empty value
        response.addHeader("TEST-ADD-HEADER_EMPTY-VALUE", "");

        sos.println("Ran [" + method + "]");
        LOG("<<<< TESTING [" + method + "]");
    }

    private void testSetHeader() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        response.setHeader("TEST-SET-HEADER", "setHeaderValue");

        //Set same header with null. Expect: header is removed, verify at the client.
        response.setHeader("TEST-SET-HEADER", null);

        //Set a header with null name. Expect: no change, no exception
        response.setHeader(null, "setHeaderNameIsNull");

        //Set a header with null name and null value. Expect: no change, no exception
        response.setHeader(null, null);

        //Set then replace with empty value
        response.setHeader("TEST-SET-HEADER_EMPTY-VALUE", "setHeaderValue2");
        response.setHeader("TEST-SET-HEADER_EMPTY-VALUE", "");

        //Add a header with multiple values, then setHeader to replace ALL values with a new one.
        response.addHeader("TEST-MULTI-ADD-THEN-SET-HEADER", "ONE_ADD_HeaderValue");
        response.addHeader("TEST-MULTI-ADD-THEN-SET-HEADER", "TWO_ADD_HeaderValue");
        LOG(method + " setHeader to replace ALL with new value");
        response.setHeader("TEST-MULTI-ADD-THEN-SET-HEADER", "FINAL_BY_SET_HeaderValue");

        //Add a header with multiple values, then setHeader with null value to remove this header.
        response.addHeader("TEST-ADD-THEN-SET-HEADER-NULL-VALUE", "THIRD_ADD_HeaderValue");
        response.addHeader("TEST-ADD-THEN-SET-HEADER-NULL-VALUE", "FOURTH_ADD_HeaderValue");
        LOG(method + " setHeader to NULL to remove ALL");
        response.setHeader("TEST-ADD-THEN-SET-HEADER-NULL-VALUE", null);

        sos.println("Ran [" + method + "]");
        LOG("<<<< TESTING [" + method + "]");
    }

    /*
     * Check response in the client side for the header
     *
     * TEST-ADD-DATE-HEADER: Fri, 30 Dec 2022 19:05:51 GMT
     * TEST-ADD-DATE-HEADER: Sat, 30 Dec 2023 19:05:51 GMT
     */
    private void testAddandSetDateHeader() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        //Fri, 30 Dec 2022 19:05:51 GMT
        final long DATE_2022 = 1672427151000L;

        //Sat, 30 Dec 2023 19:05:51 GMT
        final long DATE_2023 = 1703963151000L;

        //Tue, 09 Jan 2024 00:31:55 GMT
        final long DATE_2024 = 1704760315731L;

        //Add a date header with multiple values.
        response.addDateHeader("TEST-ADD-DATE-HEADER", DATE_2022);
        response.addDateHeader("TEST-ADD-DATE-HEADER", DATE_2023);

        //Add a null name. Expect: no change, no exception.
        response.addDateHeader(null, 123456);

        //Add setDateHeader
        response.setDateHeader("TEST-SET-DATE-HEADER", DATE_2024);

        //Set a null name. Expect: no change, no exception
        response.setDateHeader(null, 123456);

        //Add a header with multiple values; then override all with a set of same header.
        response.addDateHeader("TEST-MULTI-ADD-THEN-SET-DATE-HEADER", DATE_2022);
        response.addDateHeader("TEST-MULTI-ADD-THEN-SET-DATE-HEADER", DATE_2023);
        LOG(method + " setDateHeader to replace ALL with a new value");
        response.setDateHeader("TEST-MULTI-ADD-THEN-SET-DATE-HEADER", DATE_2024);

        sos.println("Ran [" + method + "]");
        LOG("<<<< TESTING [" + method + "]");

    }

    private void testAddandSetIntHeader() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        //Add a header with multiple values
        response.addIntHeader("TEST-ADD-INT-HEADER", 12345);
        response.addIntHeader("TEST-ADD-INT-HEADER", 67890);

        //Add a null name. Expect: no change, no exception.
        response.addIntHeader(null, 11111);

        response.setIntHeader("TEST-SET-INT-HEADER", 98765);

        //Set a null name. Expect: no change, no exception.
        response.setIntHeader(null, 22222);

        //Add a header with multiple values, then override ALL with a set
        response.addIntHeader("TEST-MULTI-ADD-THEN-SET-INT-HEADER", 111111);
        response.addIntHeader("TEST-MULTI-ADD-THEN-SET-INT-HEADER", 222222);
        LOG(method + " setIntHeader to replace ALL with a new value");
        response.setIntHeader("TEST-MULTI-ADD-THEN-SET-INT-HEADER", 888888);

        sos.println("Ran [" + method + "]");
        LOG("<<<< TESTING [" + method + "]");
    }

    /*
     * Test request getDateHeader and getInHeader : return the first value when there are multiple headers with same name.
     * Client verifies the result from the response body.
     */
    private void testRequestGetDateAndIntHeaders() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        //prime these calls...
        request.getIntHeader("testInHeader");
        request.getDateHeader("testDateHeader");

        //...then call them again purposely to make sure the same value and report back to client
        response.setIntHeader("requestInHeader", request.getIntHeader("testInHeader"));
        response.setDateHeader("requestDateHeader", request.getDateHeader("testDateHeader"));
        response.setDateHeader("requestIfModifiedSinceHeader", request.getDateHeader("If-Modified-Since"));

        LOG("<<<< TESTING [" + method + "]");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
