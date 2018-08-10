/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor_fat.session.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/testSessionServlet")
public class TestSessionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		StringWriter body = new StringWriter();

		HttpSession session = req.getSession(true);

		Enumeration<String> attributes = session.getAttributeNames();
		body.append("Session id: " + session.getId() + "<br/>");

		String attributeName = req.getParameter("attributeName");
		String attributeValue = req.getParameter("attributeValue");
		if (attributeName != null && attributeValue != null) {
			session.setAttribute(attributeName, attributeValue);
		}
		try {
			String n = attributes.nextElement();
			while (n != null) {
				body.append("&nbsp;&nbsp;&nbsp;&nbsp;" + n + ": " + session.getAttribute(n) + "<br/>");
				n = attributes.nextElement();
			}
		} catch (NoSuchElementException nsee) {
			body.append("&nbsp;&nbsp;&nbsp;&nbsp;No more attribute!<br/>");
		}

		String sleepTime = req.getParameter("sleepTime");
		if (sleepTime != null) {
			body.append("sleepTime: " + sleepTime + "<br/>");

			int sleepTimeInMilliSecs = Integer.parseInt(req.getParameter("sleepTime"));
			try {
				Thread.sleep(sleepTimeInMilliSecs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		resp.setContentType("text/html");

		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<head><title>Test Session Head</title></head>");
		out.println("<body>Test Session Body<br/>");
		out.println(body.toString());
		out.println("</body>");
		out.println("</html>");
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
}
