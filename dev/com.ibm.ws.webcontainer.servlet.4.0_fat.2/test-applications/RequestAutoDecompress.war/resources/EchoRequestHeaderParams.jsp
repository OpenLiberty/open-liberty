<!--
    Copyright (c) 2025 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
<!-- echo all request headers and request parameters -->

<%@ page contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    out.println("Method: " + request.getMethod());
    out.println("URI: " + request.getRequestURI());
    out.println("Content-Encoding: " + request.getHeader("Content-Encoding"));
    out.println();

    out.println("=== Request Headers ===");
    java.util.Enumeration<String> headerNames = request.getHeaderNames();
    String name;
    while (headerNames.hasMoreElements()) {
        name = headerNames.nextElement();
        out.println(name + ": " + request.getHeader(name));
    }
    out.println();

    int pairNumbers = 0;
    out.println("=== Request Parameters ===");
    java.util.Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
        name = paramNames.nextElement();
        out.println(name + " = " + request.getParameter(name));
        pairNumbers++;
    }
    
    System.out.println("Total pairs [" + pairNumbers + "]");
    out.println("Total param pairs [" + pairNumbers + "]");
%>
