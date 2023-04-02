/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package uripath.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test decode and normalization of getRequestURI()
 *
 * Example of URIs
 * https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0.html#example-uris
 *
 * Use request header to pass in a test name in order to keep a clean request URI. Look for this header in the trace
 *
 * In the 400 Bad Request tests, this is not called cuz the WC rejects it early.
 */
@WebServlet("/DecodeNormalizeURI/*")
public class DecodeNormalizeURI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = DecodeNormalizeURI.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public DecodeNormalizeURI() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder sbReport = new StringBuilder(" Message - ");
        String testName = request.getHeader("TEST_NAME");

        LOG.info("Start testing [" + testName + "]");

        try {
            //display pathInfo first since it is used often
            sbReport.append("pathInfo [" + request.getPathInfo()
                            + "] , servletPath [" + request.getServletPath()
                            + "] , contextPath [" + request.getContextPath()
                            + "] , reqURL [" + request.getRequestURL().toString()
                            + "] , reqURI [" + request.getRequestURI()
                            + "]");

            LOG.info(sbReport.toString());
        } catch (Exception e) {
            LOG.info("Exception [" + e + "]");
            sbReport.append(e);
        }

        response.setHeader("TestResult", sbReport.toString());
    }
}
