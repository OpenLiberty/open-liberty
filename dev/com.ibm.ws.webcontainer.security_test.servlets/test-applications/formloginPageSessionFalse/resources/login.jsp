<%--
    Copyright (c) 2017 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 --%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page session="false" %>
<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<META name="GENERATOR" content="IBM Software Development Platform">
<TITLE>login.jsp</TITLE>
</HEAD>
<BODY>
<h2>Form Login Page Session False Page</h2>
<FORM action="j_security_check" method="POST">
UserName: <INPUT type="text" name="j_username">
Password: <INPUT type="password" name="j_password">
<br>
Description: <INPUT type="text" name="j_description">
<INPUT type="submit" name="action" value="Login">
</BODY>
</FORM>
</HTML>
