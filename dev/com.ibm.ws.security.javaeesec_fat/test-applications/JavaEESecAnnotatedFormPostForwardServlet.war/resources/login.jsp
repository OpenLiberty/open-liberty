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
contentType="text/html"
pageEncoding="ISO-8859-1"
buffer="16kb"
isThreadSafe="true"
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
  <tbody><tr><td nowrap>
    <font class="t6" size="2">&nbsp;
    The Form Login page</font>
  </td></tr>
  <tr><td nowrap>
    <font color="#0033CC" class="t6" size="-1">&nbsp;
    Please enter user ID and password.</font>
  </td></tr>
</tbody></table>

<br>
<center>
  <form method="POST" action="j_security_check">
  
  <table align="center" width="30%" cellpadding="0" cellspacing="0" border="0">
    <tbody><tr bgcolor="#ADB0EC"><td>
      <table border="0" cellspacing="0" cellpadding="5" width="100%" bgcolor="#ffffff">
        <tbody><tr bgcolor="#ffffff">
          <td nowrap bgcolor="#ADB0EC"> <font class="t6" size="2">&nbsp;<b>Login</b></font></td>
          <td valign="top" bgcolor="#ADB0EC">&nbsp;<br></td>
        </tr>
      </tbody></table>

      <table cellspacing="0" cellpadding="2" border="1" width="100%" bordercolor="#cccccc" bgcolor="#ffffff">
        <tbody><tr><td colspan="5">
          <table border="0" cellpadding="0" cellspacing="0" width="100%">
            <tbody><tr><td>
              <table border="0" cellpadding="3" cellspacing="1">
                <tbody><tr>
                  <td>
                    <label for="name">
                      <div align="right"><font size="-1">User ID:</font></div>
                    </label>
                  </td>
                  <td>
                    <input type="text" name="j_username" class="short" id="name" maxlength="40">
                  </td>
                </tr>
                <tr>
                  <td>
                    <label for="pswd">
                      <div align="right"><font size="-1">Password:</font></div>
                    </label>
                  </td>
                  <td>
                    <input type="password" name="j_password" class="short" id="pswd" maxlength="40">
                  </td>
                </tr>
              </tbody></table>
            </td></tr>
          </tbody></table>
        </td></tr>
      </tbody></table>
    </td></tr>
  </tbody></table>

  <p>
    <input type="submit" name="Submit" value="Log In">
  </p>
  </form>

</center>
                  
</body>
</html>
