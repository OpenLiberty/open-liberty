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
package testgetmapping.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This test will test the functionality of the HttpServletRequest.getMapping()
 * API.
 *
 * Multiple requests will be driven to the same servlet using different url
 * patterns to ensure that the correct ServletMapping values are returned:
 *
 */
@WebServlet(urlPatterns = { "", "/pathMatch/*", "/", "/exactMatch",
                            "*.extension" },
            name = "GetMappingTestServlet", asyncSupported = true)
public class GetMappingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("++++++++ Enter GetMappingServlet");

        HttpServletMapping mapping = request.getHttpServletMapping();

        if (mapping != null) {
            response.getWriter().append("ServletMapping values: mappingMatch: " + mapping.getMappingMatch());
            response.getWriter().append(" matchValue: " + mapping.getMatchValue());
            response.getWriter().append(" pattern: " + mapping.getPattern());
            response.getWriter().append(" servletName: " + mapping.getServletName());
        } else {
            response.getWriter().append("FAIL: mapping was null");
        }

        String completeAsync = request.getParameter("completeAsync");
        if (completeAsync != null) {
            request.getAsyncContext().complete();
        }

        System.out.println("-------- Exit GetMappingServlet");

    }

}
