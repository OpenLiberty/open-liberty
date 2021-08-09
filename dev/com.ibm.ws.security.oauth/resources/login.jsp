<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--
    Copyright (c) 2019 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->
<%@ page session="false" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Login</title>
</head>
<%
	if(request.getUserPrincipal() != null) {
		// if already authenticated
		for(Cookie cookie : request.getCookies()) {
			if("WASReqURL".equals(cookie.getName())) {
				response.sendRedirect(cookie.getValue());
				response.setHeader("Set-Cookie", "WASReqURL=\"\"");
				break;
			}
		}
	}
%>
<body>
	<h3>Enter your username and password to login</h3>
	<%
		if(request.getParameter("error") != null) {
	%>		
	<div style="color: red">Error: username and password doesn't match.</div><br>
	<%	
		}
	%>
	<form action="j_security_check" method="post">
		<table>
			<tr>
				<td>Username: </td>
				<td><input name="j_username" type="text" size="25">
				</td>
			</tr>
			<tr>
				<td>Password: </td>
				<td><input name="j_password" type="password" size="25">
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<button type="submit" name="submitButton">Login</button>
					<button type="reset" name="resetButton">Reset</button>
				</td>
			</tr>
		</table>
	</form>
</body>
</html>