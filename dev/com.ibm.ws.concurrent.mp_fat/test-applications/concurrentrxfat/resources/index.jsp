<!--
 *******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************
-->
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%@ page import="java.lang.reflect.Method" %>
<%@ page import="java.util.Map" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Test methods for ${pageContext.request.contextPath}</title>
    </head>
    <body>
        <h1>Test methods for ${pageContext.request.contextPath}</h1>
        <%
        Map<String, ? extends ServletRegistration> servletRegistrations = request.getServletContext().getServletRegistrations(); 
		for(String servlet : servletRegistrations.keySet()){  
			try{
		    	Class<?> servletClass = Class.forName(servlet); 
		    	servlet = servletClass.getSimpleName();
		    	%> <h2> Tests for <%=servlet %> </h2> <UL><%
		        for (Method m : servletClass.getMethods()) {
		            String methodName = m.getName();
		            if (methodName.startsWith("test")) {
		            	%> <li> Invoke test method: <a href="${pageContext.request.contextPath}/<%=servlet%>?test=<%= methodName %>"> <%= methodName %> </a> </li><%
		            }
		       	}
		    	%> </UL> <%
			} catch(ClassNotFoundException e){
			}
		} %>
    </body>
</html>
