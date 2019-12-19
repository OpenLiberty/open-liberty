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
package com.ibm.ws.wsatAppWithoutAssertion.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.wsatAppWithoutAssertion.utils.CommonUtils;
import com.ibm.ws.wsat.ut.util.AbstractTestServlet;

/**
 * Servlet implementation class ResultServlet
 */
@WebServlet("/ResultServlet")
public class ResultServlet extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected String get(HttpServletRequest request) throws ServletException, IOException {

		String server = request.getParameter("server");
		String method = request.getParameter("method");

		StringBuilder sb = new StringBuilder();

		try {
			if (method != null) {
				if (method.equals("init")) {
					CommonUtils.initTable(server);
				}
			}
			
			if (server != null) {
				sb.append(CommonUtils.getDBCount(server));
			} else {
				sb.append("Pease input parameter like: ?server=client&method=init for initialize or ?server=server1 for count");
			}
		} catch (Exception e) {
			sb.append("Get Exception: " + e.getMessage());
			e.printStackTrace();
		}
		
		return sb.toString();
	}
}
