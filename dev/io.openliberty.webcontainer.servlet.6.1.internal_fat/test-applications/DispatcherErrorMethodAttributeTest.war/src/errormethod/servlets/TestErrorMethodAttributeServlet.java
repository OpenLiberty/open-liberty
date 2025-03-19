/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package errormethod.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test the method values for different dispatches as show in the diagram:
 *
 * Initial Request method is [POST] --- Dispatching-to-Error-Page ---> During error page dispatch method is [GET]
 *                                   --- return-from-Error-Dispatch --> After exited error page dispatch, method is restored to [POST]
 */
@WebServlet("/TestErrorMethodAttribute")
public class TestErrorMethodAttributeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestErrorMethodAttributeServlet.class.getName();

    public TestErrorMethodAttributeServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        LOG("ENTER doGet");

        switch (req.getHeader("runTest")) {
            case "test_ErrorMethod_Attribute" : test_ErrorMethod_Attribute(req,resp); break;
            case "test_ErrorQueryString_Attribute" : test_ErrorQueryString_Attribute(req,resp); break;
        }

        LOG("EXIT doGet");
    }

    private void test_ErrorMethod_Attribute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        String originalMethod = req.getMethod();
        String afterErrorDispatchMethod = null;

        LOG(" Request method before calling into sendError , method [" + originalMethod + "]");

        //save it in the request attribute so that it can be compared and verified during the error-page handling
        req.setAttribute("REQUEST_METHOD", originalMethod);

        try {
            resp.sendError(501, "Testing send error method attribute with a 501 status code!");

            LOG(" AFTER sendError, request method is [" + (afterErrorDispatchMethod = req.getMethod()) +"] ; it is compared to the original request method ["+ originalMethod + "]");

            //Ideally, we want to report the result back to the client if the request method is not reset after
            //return to the caller method.  However, there is not a way to do this after sendError, since the response is committed and closed.
            //Thus this servlet will throw a RuntimeException to fail the test ONLY IF the methods are not matched.  If the methods
            //match, NO report.
            //Test framework will then detect a FFDC/Warning and fail this test.
            if (!originalMethod.equalsIgnoreCase(afterErrorDispatchMethod)) {
                throw new RuntimeException("Found method [" + afterErrorDispatchMethod + "] AFTER returned from error dispatch but expected [" + originalMethod + "]");
            }
        }
        catch (Exception e) {
            LOG(" Exception [" + e + "]");
            throw e;
        }

        LOG("<<< TESTING [" + method + "]");
    }

    /*
     * set request attribute ERROR_QUERY_STRING so that the ErrorPageServlet can determine which test to run.
     * When run this test, ErrorPageServlet needs to:
     * 1. retrieve the request attribute "jakarta.servlet.error.query_string",
     * 2. append the retrieved value to the response which the client will compare with what was sent.
     */
    private void test_ErrorQueryString_Attribute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");

        //set attribute for ErrorPageServlet to run this test.
        req.setAttribute("ERROR_QUERY_STRING", "run this test");
        resp.sendError(501, "SendError 501 to test jakarta.servlet.error.query_string attribute!");

        LOG("<<< TESTING [" + method + "]");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
