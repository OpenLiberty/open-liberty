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
package testgetmapping.war.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
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
@WebServlet(urlPatterns = { "/pathIncMatch" }, name = "GetMappingIncServlet")
public class GetMappingIncServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String dispatchPath = request.getParameter("dispatchPath");

		System.out.println(">>>>>>>> Enter GetMappingIncServlet : dispatch to : " + dispatchPath);
		RequestDispatcher rd = request.getRequestDispatcher(dispatchPath);
		rd.include(request, response);
		System.out.println("<<<<<<<< Exit GetMappingIncServlet");

	}

}
