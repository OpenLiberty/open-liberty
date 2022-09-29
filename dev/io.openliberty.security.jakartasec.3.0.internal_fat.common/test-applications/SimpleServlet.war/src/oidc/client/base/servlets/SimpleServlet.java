/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.base.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public SimpleServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();

        // TODO - add code to use the openIdContextBean to dump content for test cases to check - would like to use this same app for protected and unprotected, so skip the full logging if the context bean is null
        recordHelloWorld(outputStream);
    }

    protected void recordHelloWorld(ServletOutputStream output) throws IOException {

        System.out.println("Hello world from SimpleServlet");
        output.println("Hello world from SimpleServlet!");

    }
}
