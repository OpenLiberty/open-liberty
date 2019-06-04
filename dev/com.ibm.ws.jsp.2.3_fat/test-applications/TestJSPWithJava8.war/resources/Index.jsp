<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<!DOCTYPE HTML><%@page import="java.io.PrintWriter"%>
<%@page import="java.io.Writer"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page language="java"
    contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
<title>Java 8</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>

<%
List<String> messages = Arrays.asList("one", "two", "three", "four");
try {
  PrintWriter write = response.getWriter();
  messages.stream().forEach(e -> write.print(e));
} catch (Exception e) {
  e.printStackTrace();
}
 %>

</body>
</html>
