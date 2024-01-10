/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package httpcodes.servlets;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test new HTTP response status code 308, 421, 422, 426
 */
@WebServlet("/TestHttpResponseCodes")
public class TestHttpResponseCodesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestHttpResponseCodesServlet.class.getName();
    String encoding;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        StringBuilder sBuilder = new StringBuilder();
        ServletContext context = getServletContext();

        testHttpResponseCodes(response, sBuilder);

        sos.println("Test Results from " + CLASS_NAME + ": \n" + sBuilder.toString());
    }

    private void testHttpResponseCodes(HttpServletResponse response, StringBuilder sBuilder) {
        LOG(">>> Testing testHttpResponseCodes");
        String result;

        result = "test HttpServletResponse.SC_PERMANENT_REDIRECT 308 code [" + (HttpServletResponse.SC_PERMANENT_REDIRECT == 308 ? "PASS" : "FAIL") + "]\n";
        LOG(result);
        sBuilder.append(result);

        result = "test HttpServletResponse.SC_MISDIRECTED_REQUEST 421 code [" + (HttpServletResponse.SC_MISDIRECTED_REQUEST == 421 ? "PASS" : "FAIL") + "]\n";
        LOG(result);
        sBuilder.append(result);

        result = "test HttpServletResponse.SC_UNPROCESSABLE_CONTENT 422 code [" + (HttpServletResponse.SC_UNPROCESSABLE_CONTENT == 422 ? "PASS" : "FAIL") + "]\n";
        LOG(result);
        sBuilder.append(result);

        result = "test HttpServletResponse.SC_UPGRADE_REQUIRED 426 code [" + (HttpServletResponse.SC_UPGRADE_REQUIRED == 426 ? "PASS" : "FAIL") + "]\n";
        LOG(result);
        sBuilder.append(result);
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
