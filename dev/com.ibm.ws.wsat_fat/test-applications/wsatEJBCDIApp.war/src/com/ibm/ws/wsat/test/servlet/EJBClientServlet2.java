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
package com.ibm.ws.wsat.test.servlet;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.wsat.ut.util.AbstractTestServlet;

/**
 * Servlet implementation class ClientServlet
 */
@WebServlet("/EJBClientServlet2")
public class EJBClientServlet2 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	
	@EJB
	EJBClient2 ejbClient2;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected String get(HttpServletRequest request) throws ServletException, IOException {
		return ejbClient2.invokeCall(request);	
	}
}
