package com.ibm.ws.io.smallrye.graphql.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GraphiQLUIServlet extends HttpServlet {
	private static final long serialVersionUID = 333566789521908L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		req.getServletPath()
	}
}
