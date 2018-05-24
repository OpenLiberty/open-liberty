/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import javax.ws.rs.core.MediaType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.ApplicationProcessor.DocType;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

public class OpenAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(OpenAPIServlet.class);

    /** {@inheritDoc} */

    /** {@inheritDoc} */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ApplicationProcessor applicationProcessor = ApplicationProcessor.getInstance();

        if (request.getMethod().equals(Constants.METHOD_GET)) {
            if (applicationProcessor == null) {
                Writer writer = response.getWriter();
                writer.write("Failed to find OpenAPI application processor");
                response.setStatus(404);
            }

            String acceptHeader = "";
            acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
            String format = "yaml";
            if (acceptHeader != null && acceptHeader.equals(MediaType.APPLICATION_JSON)) {
                format = "json";
            }
            String formatParam = request.getParameter("format");
            if (formatParam != null && formatParam.equals("json")) {
                format = "json";
            }

            response.setCharacterEncoding("UTF-8");
            if (format.equals("json")) {
                String document = applicationProcessor.getOpenAPIDocument(request, DocType.JSON);
                if (document != null) {
                    response.setContentType(MediaType.APPLICATION_JSON);
                    Writer writer = response.getWriter();
                    writer.write(document);
                } else {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Null document (json). Return 500.");
                    }
                    response.setStatus(500);
                }
            } else {
                String document = applicationProcessor.getOpenAPIDocument(request, DocType.YAML);
                if (document != null) {
                    response.setContentType(MediaType.TEXT_PLAIN);
                    Writer writer = response.getWriter();
                    writer.write(document);
                } else {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Null document (yaml). Return 500.");
                    }
                    response.setStatus(500);
                }
            }
        } else {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Invalid method. Return 405.");
            }
            response.setStatus(405);
        }

    }
}
