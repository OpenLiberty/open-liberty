/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package getmapping.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This test will test the functionality of the HttpServletRequest.getMapping()
 */
@WebServlet(urlPatterns = { "", "/path/*", "/", "/MyServlet", "*.extension" }, name = "MyServlet")
public class GetMappingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = GetMappingServlet.class.getName();

    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;

    public GetMappingServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        LOG("ENTER doGet");

        testGetMapping();

        LOG(responseSB.toString());
    	sos.println(responseSB.toString());

        LOG("EXIT doGet");
    }

    private void testGetMapping() throws IOException{
    	LOG(">>> testGetMapping");

    	HttpServletMapping mapping = request.getHttpServletMapping();

    	if (mapping != null) {
    	    responseSB.append("Request URI [" + request.getRequestURI() + "]");
    	    responseSB.append("\n ServletMapping values:");
    	    responseSB.append(" mappingMatch [" + mapping.getMappingMatch()+"]");
    	    responseSB.append(" matchValue [" + mapping.getMatchValue()+"]");
    	    responseSB.append(" pattern [" + mapping.getPattern()+"]");
    	    responseSB.append(" servletName [" + mapping.getServletName()+"]");
    	} else {
    	    responseSB.append("FAIL: mapping was null");
    	}
    	LOG("<<< testGetMapping");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
