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

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test request attribute "jakarta.servlet.error.method" under which the method of the request whose processing caused the error is
 * propagated during an error dispatch.
 *
 * Also test the actual method during the error dispatch is "GET"
 *
 * Request method is POST --Dispatching-to-Error-Page--> during error page dispatch method is GET --return-from-from-Error-Dispatch--> after exited error page dispach, method is POST.
 *
 * Also test the request attribute "jakarta.servlet.error.query_string attribute"
 */

@WebServlet(urlPatterns = {"/ErrorPageServlet/*"}, name = "ErrorPageServlet")
public class ErrorPageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = ErrorPageServlet.class.getName();

    public ErrorPageServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        LOG("ENTER doGet");

        StringBuilder responseSB = new StringBuilder();
        ServletOutputStream sos = resp.getOutputStream();

        boolean testErrorQueryString = ((request.getAttribute("ERROR_QUERY_STRING") != null) ? true : false);

        if (testErrorQueryString) {     //test error query string attribute

            LOG(" checking for request attribute jakarta.servlet.error.query_string from error dispatch");
            responseSB.append(" Error Page request.getAttribute(\"" + RequestDispatcher.ERROR_QUERY_STRING + "\")" );

            String qsAttributeValue = (String) request.getAttribute(RequestDispatcher.ERROR_QUERY_STRING) ;
            if (qsAttributeValue != null) {
                responseSB.append(" . Found attribute with value [" + qsAttributeValue + "]");
            }
        }
        else {                          //test error method attribute
            String errorMethodAtt;          // request error.method attribute which is the original method BEFORE going into sendError
            String dispatchingMethod;       // method DURING the sendError().  This should always be "GET"
            String initialRequestMethod;    // saved original request method before the dispatch; it is used to compare with errorMethodAtt

            LOG(" Initial request method [" + (initialRequestMethod = (String) request.getAttribute("REQUEST_METHOD")) + "]\n");
            LOG(" Error method attribute : from request dispatch attribute jakarta.servlet.error.method [" + (errorMethodAtt = (String) request.getAttribute(RequestDispatcher.ERROR_METHOD)) + "]\n");
            LOG(" Method during error-page handling path (which is this ErrorPageServlet) [" + (dispatchingMethod = request.getMethod()) + "]\n");

            responseSB.append(" Response from ErrorPageServlet\n");

            if (errorMethodAtt.equalsIgnoreCase(initialRequestMethod)) {
                responseSB.append("Test request dispatcher jakarta.servlet.error.method attribute [PASS]\n");
            }
            else {
                responseSB.append("Test request dispatcher jakarta.servlet.error.method attribute [FAIL]. Expected the initial request method [" +initialRequestMethod +"] in the jakarta.servlet.error.method attribute but found ["+errorMethodAtt+"]\n");
            }

            if (dispatchingMethod.equals("GET")) {
                responseSB.append("Test error dispatch method [PASS]\n");
            }
            else {
                responseSB.append("Test error dispatch method[FAIL] . Expected [GET] but found [" + dispatchingMethod + "]\n");
            }
        }

        LOG(responseSB.toString());
        sos.println(responseSB.toString());

        LOG("EXIT doGet");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
