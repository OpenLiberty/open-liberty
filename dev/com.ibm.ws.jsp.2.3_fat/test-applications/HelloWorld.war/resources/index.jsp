<!--
    Copyright (c) 2025 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
 <%@ page import="java.util.stream.Collectors,java.util.stream.Stream" %>
 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
 <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
 <html>
 <head>
 
 <title>Test Debug Support</title>
 </head>
 <body>

    Hello world!
 
     <p> Testing JSTL </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="i" begin="1" end="2">  
             Item <c:out value="${i}"/>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing a Scriptlet </p>
 
     <%
     String context = request.getContextPath();
     String testString = "saying Hello from page " + context;
     out.print(testString);
     %>
 
     <p> Testing Lambdas </p>
     <%
     char[] numbersChars = {'t','e', 's', 't'};
     String testStringLambda = Stream.of(numbersChars).map(arr ->  new String(arr)).collect(Collectors.joining());
     out.print(testStringLambda);
     %>
 
     
 </body>
 </html>
