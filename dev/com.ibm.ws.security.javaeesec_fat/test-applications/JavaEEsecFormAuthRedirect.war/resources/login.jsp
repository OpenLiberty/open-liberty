<%--  Copyright (c) 2017, 2018 IBM Corporation and others.                   --%>
<%--  All rights reserved. This program and the accompanying materials       --%>
<%--  are made available under the terms of the Eclipse Public License 2.0  --%>
<%--  which accompanies this distribution, and is available at               --%>
<%--  http://www.eclipse.org/legal/epl-2.0/                              --%>
<%--                                                                         --%>
<%--  Contributors:                                                          --%>
<%--      IBM Corporation - initial API and implementation                   --%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>login page for the form login test</title>
</head>
<body>
<h2>Form Login Page</h2>
<form action="j_security_check" method="POST">
UserName: <input type="text" name="j_username">
Password: <input type="password" name="j_password">
<input type="submit" name="action" value="Login">
</form>
</body>
</html>