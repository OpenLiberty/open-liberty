<!--
  Copyright (c) 2021 IBM Corporation and others.
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
<html>
<head>
<title>ReLogin</title>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
</head>
<body>

<FORM method='post' action='j_security_check'>
	<TABLE border='0' width='360'>
			<TR>
				<TD width='220'>Username</TD>
				<TD width='140'><input type='text' name='j_username' maxlength='20' size='25' value=''></TD>
			</TR>
			<TR>
				<TD>Password</TD>
				<TD><input type='password' name='j_password' maxlength='12' size='18'></TD>
			</TR>
			<TR>
				<TD><BR><INPUT type='submit' value='   Login   '></TD>
			<TR>
	</TABLE>
</FORM>
</body>
</html>
