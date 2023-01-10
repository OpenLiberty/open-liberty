/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.app.manager.war.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(description = "A servlet to drive the test EJB beans", urlPatterns = { "/EjbNoDD" })
public class EjbNoDD extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    private InterfaceTestStatelessEjbNoDD test1;

    @EJB
    private InterfaceTestStatefulEjbNoDD test2;

    @EJB
    private InterfaceTestSingletonEjbNoDD test3;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        test(request, response);
    }

    private void test(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println("Testing EJB with no DD...");
        String message = this.test1.test();
        writer.println(message);
        message = this.test2.test();
        writer.println(message);
        message = this.test3.test();
        writer.println(message);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        test(request, response);
    }
}
