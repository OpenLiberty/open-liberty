/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.apps.postConstructError;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

@WebServlet("/errorMessageTestServlet")
public class ErrorMessageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    ErrorMessageTestEjb errEjb;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        errEjb.doSomething();
        PrintWriter pw = response.getWriter();
        pw.write("Hello World");
    }

}
