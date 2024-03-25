/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package response.noop.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A test Servlet to ensure that specific methods in the ServletResponse and HttpServletResponse perform
 * no action once a response has been committed.
 */
@WebServlet("/TestResponseNoOpAfterCommit")
public class TestResponseNoOpAfterCommitServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestResponseNoOpAfterCommitServlet.class.getName();

    HttpServletResponse response;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        response = resp;

        LOG("ENTER doGet");

        String runTestMethod = req.getHeader("runTest");

        if (runTestMethod.equalsIgnoreCase("ServletResponse_setContentLength")) {
            testServletResponse_setContentLength();
        } else if (runTestMethod.equalsIgnoreCase("ServletResponse_setContentLengthLong")) {
            testServletResponse_setContentLengthLong();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_addCookie")) {
            testHttpServletResponse_addCookie();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_setHeader")) {
            testHttpServletResponse_setHeader();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_setIntHeader")) {
            testHttpServletResponse_setIntHeader();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_addIntHeader")) {
            testHttpServletResponse_addIntHeader();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_setStatus")) {
            testHttpServletResponse_setStatus();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_setDateHeader")) {
            testHttpServletResponse_setDateHeader();
        } else if (runTestMethod.equalsIgnoreCase("HttpServletResponse_addDateHeader")) {
            testHttpServletResponse_addDateHeader();
        }

        LOG("EXIT doGet");
    }

    /*
     * Test that the ServletResponse.setContentLength method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testServletResponse_setContentLength() throws IOException {
        LOG(">>> Testing testServletResponse_setContentLength");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testServletResponse_setContentLength!");
        stream.flush();
        response.setContentLength(10);

        LOG("<<< Testing testServletResponse_setContentLength");

    }

    /*
     * Test that the ServletResponse.setContentLengthLong method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testServletResponse_setContentLengthLong() throws IOException {
        LOG(">>> Testing testServletResponse_setContentLengthLong");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testServletResponse_setContentLengthLong!");
        stream.flush();
        response.setContentLengthLong(10);

        LOG("<<< Testing testServletResponse_setContentLengthLong");

    }

    /*
     * Test that the HttpServletResponse.addCookie method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_addCookie() throws IOException {
        LOG(">>> Testing testHttpServletResponse_addCookie");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_addCookie!");
        stream.flush();

        Cookie cookie = new Cookie("CookieName", "CookieValue");
        response.addCookie(cookie);

        LOG("<<< Testing testHttpServletResponse_addCookie");

    }

    /*
     * Test that the HttpServletResponse.setHeader method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_setHeader() throws IOException {
        LOG(">>> Testing testHttpServletResponse_setHeader");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_setHeader!");
        stream.flush();

        response.setHeader("TestHeaderName","TestHeaderValue");

        LOG("<<< Testing testHttpServletResponse_setHeader");

    }

    /*
     * Test that the HttpServletResponse.setIntHeader method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_setIntHeader() throws IOException {
        LOG(">>> Testing testHttpServletResponse_setIntHeader");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_setIntHeader!");
        stream.flush();

        response.setIntHeader("TestIntHeaderName",10);

        LOG("<<< Testing testHttpServletResponse_setIntHeader");

    }

    /*
     * Test that the HttpServletResponse.addIntHeader method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_addIntHeader() throws IOException {
        LOG(">>> Testing testHttpServletResponse_addIntHeader");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_addIntHeader!");
        stream.flush();

        response.addIntHeader("TestAddHeaderName",10);

        LOG("<<< Testing testHttpServletResponse_addIntHeader");

    }

    /*
     * Test that the HttpServletResponse.setStatus method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_setStatus() throws IOException {
        LOG(">>> Testing testHttpServletResponse_setStatus");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_setStatus!");
        stream.flush();

        response.setStatus(200);

        LOG("<<< Testing testHttpServletResponse_setStatus");

    }

    /*
     * Test that the HttpServletResponse.setDateHeader method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_setDateHeader() throws IOException {
        LOG(">>> Testing testHttpServletResponse_setDateHeader");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_setDateHeader!");
        stream.flush();

        //Tue, 09 Jan 2024 00:31:55 GMT
        response.setDateHeader("TEST_DATE_HEADER", 1704760315731L);

        LOG("<<< Testing testHttpServletResponse_setDateHeader");

    }

    /*
     * Test that the HttpServletResponse.addDateHeader method is a no-op once a response has been committed.
     * A call to the flush() method will commit the response.
     */
    private void testHttpServletResponse_addDateHeader() throws IOException {
        LOG(">>> Testing testHttpServletResponse_addDateHeader");

        ServletOutputStream stream = response.getOutputStream();
        stream.print("Hello from testHttpServletResponse_addDateHeader!");
        stream.flush();

        //Tue, 09 Jan 2024 00:31:55 GMT
        response.addDateHeader("TEST_DATE_HEADER", 1704760315731L);

        LOG("<<< Testing testHttpServletResponse_addDateHeader");

    }

    private static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
