/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OpenAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String acceptHeader = "";
        String method = "";
        acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
        method = request.getMethod();
        response.setContentType(Constants.CONTENT_TYPE_JSON);
        response.setCharacterEncoding("UTF-8");
        Writer writer = response.getWriter();
        writer.write("{\"openapi:\" \"3.0.0\"}");
    }
}
