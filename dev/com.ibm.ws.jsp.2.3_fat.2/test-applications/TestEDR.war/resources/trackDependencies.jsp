<!--
    Copyright (c) 2021 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <%@ taglib uri = "http://java.sun.com/jsp/jstl/core" prefix = "c" %>
        <title>Test trackDependencies</title>
    </head>
    <body>
        <p>This tests trackDependencies under concurrent requests.</p>

        <!-- Slow Down JSP Translation By Just a Bit  -->
        <c:forEach var = "i" begin = "1" end = "5"> </c:forEach>
        <c:forEach var = "i" begin = "1" end = "5"> </c:forEach>

        <%@ include file="header1.jsp" %>
        <%@ include file="headerEDR1.jsp" %>
    </body>
</html>
