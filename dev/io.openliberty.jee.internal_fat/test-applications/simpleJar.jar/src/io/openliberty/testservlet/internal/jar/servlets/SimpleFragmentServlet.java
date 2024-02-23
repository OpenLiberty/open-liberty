/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.testservlet.internal.jar.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import componenttest.app.FATServlet;

/**
 * This is just a simple Hello World servlet that we can use to drive requests to so that we can
 * initialize applications. This should be shared with other application so we don't have a ton
 * of Hello World servlets around.
 */
@WebServlet("/SimpleFragmentServlet")
public class SimpleFragmentServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    public SimpleFragmentServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        sos.println("Hello World (Fragment)");
        sos.println("[SUCCESS]");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Test
    public void simpleTest() throws Exception {
        // NO-OP: As long as this can be invoked, the test
        //        has passed.  We are mostly interested in whether
        //        the application started, which tests parsing
        //        using the new schemas.
    }
}
