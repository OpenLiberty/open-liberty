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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.ApplicationProcessor.DocType;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

public class OpenAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(OpenAPIServlet.class);

    private volatile ApplicationProcessor applicationProcessor = null;

    /** {@inheritDoc} */

    /** {@inheritDoc} */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getMethod().equals(Constants.METHOD_GET)) {
            if (applicationProcessor == null) {
                applicationProcessor = findApplicationProcessor(request);
                if (applicationProcessor == null) {
                    Writer writer = response.getWriter();
                    writer.write("Failed to find OpenAPI application processor");
                    response.setStatus(404);
                }
            }
            String acceptHeader = "";
            acceptHeader = request.getHeader(Constants.ACCEPT_HEADER);
            String format = "yaml";
            if (acceptHeader != null && acceptHeader.equals(Constants.CONTENT_TYPE_JSON)) {
                format = "json";
            }
            String formatParam = request.getParameter("format");
            if (formatParam != null && formatParam.equals("json")) {
                format = "json";
            }

            response.setCharacterEncoding("UTF-8");
            if (format.equals("json")) {
                response.setContentType(Constants.CONTENT_TYPE_JSON);
                Writer writer = response.getWriter();
                writer.write(applicationProcessor.getOpenAPIDocument(DocType.JSON));
            } else {
                Writer writer = response.getWriter();
                writer.write(applicationProcessor.getOpenAPIDocument(DocType.YAML));
            }
        } else {
            response.setStatus(405);
        }

    }

    /**
     * @param request
     */
    private ApplicationProcessor findApplicationProcessor(HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext sc = session.getServletContext();
        BundleContext ctxt = (BundleContext) sc.getAttribute("osgi-bundlecontext");

        ServiceReference<ApplicationProcessor> ref = ctxt.getServiceReference(ApplicationProcessor.class);
        if (ref == null) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to find OpenAPI Application Processor");
            }
            return null;
        } else {
            return ctxt.getService(ref);
        }
    }
}
