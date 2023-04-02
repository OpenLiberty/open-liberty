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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.apps.testmarker;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet used to log test boundaries for debugging purposes.
 */
public class TestStepMarker extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String PARAM_ACTION = "action";
    public static final String PARAM_TEST_STEP = "testStep";

    public TestStepMarker() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println(request.getParameter(PARAM_ACTION) + " TEST Step: " + request.getParameter(PARAM_TEST_STEP));
        System.out.println("-----------------------------------------------------------------------------------------");
    }
}
