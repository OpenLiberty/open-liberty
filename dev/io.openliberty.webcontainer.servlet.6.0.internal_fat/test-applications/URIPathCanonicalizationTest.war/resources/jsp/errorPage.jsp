<!--
  Copyright (c) 2023 IBM Corporation and others.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License 2.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-2.0/
    
  SPDX-License-Identifier: EPL-2.0
 -->
<%@ page  import="java.io.IOException" %>
<%@ page  import="java.io.PrintWriter" %>
<%@ page  import="java.io.OutputStreamWriter" %>
<%
	PrintWriter localOut;
	try {
		localOut = response.getWriter();
	} catch (IllegalStateException e) {
		localOut = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding()));
	}
	String message = (String) request.getAttribute("jakarta.servlet.error.message");

	localOut.println("<H1>Error Page JSP </H1>\n" + "<H4>Exception: " + message + "</H4>");
%>
