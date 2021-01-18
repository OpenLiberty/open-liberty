<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.net.URLEncoder"%>
<html>
<head>
<title>redirectToIdP</title>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <meta http-equiv="Pragma" content="no-cache">
    <title>Security Login Page </title>
</head>
<body>
<%
		String ssoURL = request.getParameter("Sso_URL");
		String PartnerId = request.getParameter("PartnerId");
		String Target = request.getParameter("target");
		//String NameIdFormat = request.getParameter("NameIdFormat");
	//	String AllowCreate = request.getParameter("AllowCreate");
	//	String RequestBinding = request.getParameter("RequestBinding");
		
		StringBuffer sb = new StringBuffer(ssoURL);		
			sb.append("?");
			sb.append("RequestBinding=HTTPPost");
			
		if (PartnerId != null && !PartnerId.isEmpty()){
			PartnerId = java.net.URLEncoder.encode(PartnerId, "UTF-8");
			sb.append("&PartnerId=");
			sb.append(PartnerId);
		}
		if (Target != null && !Target.isEmpty()){
			Target = java.net.URLEncoder.encode(Target, "UTF-8");
			sb.append("&Target=");
			sb.append(Target);
		}
		/*if (NameIdFormat !=null && !NameIdFormat.isEmpty()){
			sb.append("&NameIdFormat=");
			sb.append(NameIdFormat);
		}
		if (AllowCreate != null && !AllowCreate.isEmpty()){
			sb.append("&AllowCreate=");
			sb.append(AllowCreate);
		}*/
		
		String idpUrl = sb.toString();
		
		response.sendRedirect("idp.jsp");		
%>



</body>
</html>