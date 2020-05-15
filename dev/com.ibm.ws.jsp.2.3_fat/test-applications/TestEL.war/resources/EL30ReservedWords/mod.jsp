<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page import="com.ibm.ws.jsp23.fat.testel.beans.EL30ReserverdWordsTestBean"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test the EL 3.0 Reserved Words</title>
</head>
<body>

	<%-- Create an instance of the ReservedWordsTestBean --%>
    <%!
    	EL30ReserverdWordsTestBean test = new EL30ReserverdWordsTestBean();
    %>
    <%   
    	request.setAttribute("test", test);
    %>
	
	<%-- Test the EL 3.0 "mod" Reserved Word --%>
	${test.mod}
	
</body>
</html>
