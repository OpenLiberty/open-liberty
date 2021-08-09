<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
	
	import="java.util.Map"
%>
<%
	// Used as a common redirect landing page, so a single client can easily support multiple OAuth flows
	
	String code = request.getParameter("code");
	if (code != null) {
		pageContext.forward("HTMLUnit_client.jsp");
	}
	
	Map<String, String[]> parameters = request.getParameterMap();
	if (parameters == null || parameters.size() == 0) {
		// no parameters, probably got # arguments from implicit flow
		pageContext.forward("implicit.jsp");
	}
	
	// No other cases, only auth code and implicit have redirect URIs

%>
Something went wrong redirecting to redirect.jsp
