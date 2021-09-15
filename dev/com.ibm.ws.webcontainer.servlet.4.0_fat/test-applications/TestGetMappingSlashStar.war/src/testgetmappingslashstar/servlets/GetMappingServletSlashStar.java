/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testgetmappingslashstar.servlets;

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
 * This test expands the main TestGetMapping.war to cover the /* mapping.  We 
 * can't use the TestGetMapping.war because it has the default / servlet mapping which
 * will be unreachable by the /* mapping in this test.
 *
 */
@WebServlet(urlPatterns = "/*", name = "GetMappingTestServletSlashStar")
public class GetMappingServletSlashStar extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("++++++++ Enter GetMappingServletSlashStar");

        HttpServletMapping mapping = request.getHttpServletMapping();

        if (mapping != null) {
            response.getWriter().append("ServletMapping values: mappingMatch: " + mapping.getMappingMatch());
            response.getWriter().append(" matchValue: " + mapping.getMatchValue());
            response.getWriter().append(" pattern: " + mapping.getPattern());
            response.getWriter().append(" servletName: " + mapping.getServletName());
        } else {
            response.getWriter().append("FAIL: mapping was null");
        }

        System.out.println("-------- Exit GetMappingServlet");

    }

}
