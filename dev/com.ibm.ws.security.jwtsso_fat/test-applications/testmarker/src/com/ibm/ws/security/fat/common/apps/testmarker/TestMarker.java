/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.apps.testmarker;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used to log test boundaries for debugging purposes.
 */
public class TestMarker extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String PARAM_ACTION = "action";
    public static final String PARAM_TEST_NAME = "testCaseName";

    public TestMarker() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println(request.getParameter(PARAM_ACTION) + " TEST CASE: " + request.getParameter(PARAM_TEST_NAME));
        System.out.println("-----------------------------------------------------------------------------------------");
    }
}
