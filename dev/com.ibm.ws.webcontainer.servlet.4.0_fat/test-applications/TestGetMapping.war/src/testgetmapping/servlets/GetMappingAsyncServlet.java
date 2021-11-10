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

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Scott
 *
 */
@WebServlet(urlPatterns = "/pathAsyncMatch", name = "GetMappingAsyncServlet", asyncSupported = true)
public class GetMappingAsyncServlet extends HttpServlet {
	private static final long serialVersionUID = 2722861779284005300L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String dispatchPath = request.getParameter("dispatchPath");

		System.out.println(">>>>>>>> Enter GetMappingAsyncServlet : dispatch to : " + dispatchPath);
		AsyncContext context = request.startAsync(request, response);
		context.dispatch(dispatchPath + "?completeAsync=true");
		;
		System.out.println("<<<<<<<< Exit GetMappingAsyncServlet");

	}
}
