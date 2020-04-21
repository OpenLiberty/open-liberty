<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@page import="java.util.Arrays"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Collection"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.util.Enumeration"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test Servlet 3.1 Request and Response</title>
</head>
<body>

	<%-- Request tests --%>
	<% 
		// Testing the PrintWriter from the response object
		PrintWriter sos = response.getWriter();
		sos.print("JSP to test Servlet 3.1 Request and Response<br/>"); 
	
		sos.print("Testing BASIC_AUTH static field from HttpServletRequest (Expected: BASIC): " + HttpServletRequest.BASIC_AUTH + "<br/>");
		
		Enumeration<String> epn = request.getParameterNames();
		sos.print("Testing request.getParameterNames method (Expected: [firstName, lastName]): " + Collections.list(epn) + "<br/>");

		sos.print("Testing request.getParameter method (Expected: John): " + request.getParameter("firstName") + "<br/>");
		
		sos.print("Testing request.getParameter method (Expected: Smith): " + request.getParameter("lastName") + "<br/>");
		
		sos.print("Testing request.getQueryString method (Expected: firstName=John&lastName=Smith): " + request.getQueryString() + "<br/>");
		
		sos.print("Testing request.getContextPath method (Expected: /TestServlet): " + request.getContextPath() + "<br/>");
		
		sos.print("Testing request.getRequestURI method (Expected: /TestServlet/Servlet31RequestResponseTest.jsp): " + request.getRequestURI() + "<br/>");
		
		sos.print("Testing request.getMethod method (Expected: GET): " + request.getMethod() + "<br/>");

		sos.print("Testing request.getContentLengthLong method (Expected: -1): " + request.getContentLengthLong() + "<br/>");		
		
		sos.print("Testing request.getProtocol method (Expected: HTTP/1.1): " + request.getProtocol() + "<br/>");
	
	%>
	
	<%-- Response tests --%>
	<%	
	
		// Testing the response.setContentLengthLong method
		long length = 10000;
		response.setContentLengthLong(length);
		
		sos.print("Testing SC_NOT_FOUND static field from HttpServletResponse (Expected: 404): " + HttpServletResponse.SC_NOT_FOUND + "<br/>");
		
		sos.print("Testing response.getStatus method (Expected: 200): " + response.getStatus() + "<br/>");
	
		sos.print("Testing response.getBufferSize method (Expected: 4096): " + response.getBufferSize() + "<br/>");
		
		sos.print("Testing response.getCharacterEncoding method (Expected: ISO-8859-1): " + response.getCharacterEncoding() + "<br/>");
		
		sos.print("Testing response.getContentType method (Expected: text/html; charset=ISO-8859-1): " + response.getContentType() + "<br/>");

		sos.print("Testing response.containsHeader method (Expected: true): " + response.containsHeader("Content-Type") + "<br/>");
		
		sos.print("Testing response.isCommitted method (Expected: false): " + response.isCommitted() + "<br/>");

	%>

</body>
</html>