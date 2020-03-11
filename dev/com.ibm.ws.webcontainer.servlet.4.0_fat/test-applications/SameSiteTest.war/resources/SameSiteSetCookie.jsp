<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <title>SameSiteSetCookieJSP</title>
    </head>
    <body>
        <%
        out.println("SameSite Set-Cookie JSP Test!");

        if(response.containsHeader("Set-Cookie")) {
            out.println("Response contained a Set-Cookie header and we are replacing them: ");
        }

        for(String header:response.getHeaders("Set-Cookie")) {
            out.println(header);
        }


        // We will call setHeader before addHeader so we can test both methods.
        // If there is already a Set-Cookie header in the response we will overwrite it but for
        // testing purposes this should not be an issue.
        response.setHeader("Set-Cookie" , "jspSetHeaderCookie=jspSetHeaderCookie; Secure; SameSite=None");
        response.addHeader("Set-Cookie" , "jspAddHeaderCookie=jspAddHeaderCookie; Secure; SameSite=None");

        %>

    </body>
</html>