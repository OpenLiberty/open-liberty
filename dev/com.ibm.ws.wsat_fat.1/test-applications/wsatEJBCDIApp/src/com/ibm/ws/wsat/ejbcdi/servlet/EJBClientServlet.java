/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.ejbcdi.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;


/**
 * Servlet implementation class ClientServlet
 */
@WebServlet("/EJBClientServlet")
public class EJBClientServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@EJB
	EJBClient ejbClient;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */

    private static String TEST_NAME_PARAM = "testName";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("Servlet: " + request.getRequestURI());
        System.out.println("Test: " + request.getParameter(TEST_NAME_PARAM));

        final Enumeration<?> params = request.getParameterNames();

        while (params.hasMoreElements()) {
            final String param = (String) params.nextElement();

            if (!TEST_NAME_PARAM.equals(param)) {
                System.out.println(param + ": " + request.getParameter(param));
            }
        }

        final String result = get(request);
        
        response.getWriter().println(result);
    }

	protected String get(HttpServletRequest request) throws ServletException, IOException {
		return ejbClient.invokeCall(request);
	}
}
