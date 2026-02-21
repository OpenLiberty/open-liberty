/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webFragment;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "DeclaredServletInWebXML", urlPatterns = { "/DeclaredServletInWebXML_IgnoreMapping" })
public class DeclaredServletInWebXML extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public DeclaredServletInWebXML() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder outputBuilder = Utils.getOutputBuilder(request);
        Utils.appendLine(outputBuilder, "Executing the servlet's service method");
        Utils.appendLine(outputBuilder, "The servlet name is " + this.getServletName());
        Utils.displayOutput(request, response);
    }
}
