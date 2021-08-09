<%--
    Copyright (c) 2018 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <meta name="GENERATOR" content="IBM Software Development Platform">
        <title>login.jsp</title>
    </head>
    <body>
        <h2>Form Login Page</h2>
        <form action="j_security_check" method="POST">
            UserName: <input type="text" name="j_username">
            Password: <input type="password" name="j_password">
            <br>
            Description: <input type="text" name="j_description">
            <input type="submit" name="action" value="Login">
       </form>
    </body>
</html>
