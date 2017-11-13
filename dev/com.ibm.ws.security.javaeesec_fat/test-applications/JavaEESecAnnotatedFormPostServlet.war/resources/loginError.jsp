/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<%@ page 
language="java"
contentType="text/html; charset=ISO-8859-1"
pageEncoding="ISO-8859-1"
buffer="16kb"
isThreadSafe="true"
isErrorPage="true"
autoFlush="true"
%>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<meta name="GENERATOR" content="IBM WebSphere Studio">
<meta http-equiv="Content-Style-Type" content="text/css">

<title>Security - Form Login</title>
</head>

<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">

<table cellpadding="5" cellspacing="5">
  <tbody><tr>
   <td><h1 class="sampjsp">Security - Form Login</h1></td>
  </tr>
</tbody></table>

<hr width="100%">

<table width="600" cellpadding="5" cellspacing="0" border="0">
  <tbody><tr>
  <td width="3%"></td>
  <td width="97%" nowrap>
    <font color="#990000" class="t6" size="2" face="geneva,arial">&nbsp;
      <b>Authentication for the Form Login failed.</b>
    </font>
  </td></tr>
    
  <tr>
    <td width="3%"></td>
    <td width="97%" nowrap>
      <font face="arial" size="-1">
        Check the user ID and password, and try again.
      </font>
      <form action="login.jsp" method="post">
        <input type="submit" value="OK">
      </form>
  </td></tr>
</tbody></table>  
   
</body>
</html>
