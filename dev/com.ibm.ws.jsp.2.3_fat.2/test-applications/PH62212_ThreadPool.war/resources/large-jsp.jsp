<?xml version="1.0" encoding="UTF-8"?>
<!--
    Copyright (c) 2024 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
 <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
 <html>
 <head>
 
 <title>Large JSP FILE</title>
 </head>
 <body>

   <p> Testing c:set, c:if, c:forEach </p>
 
   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>

   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>

    
   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>

    
   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>

    
   <c:choose>  
       <c:when test="${isTrue}">  
           c:when works!   
           <%= 1+2 %>
       </c:when>  
       <c:when test="${!isTrue}">  
           Should never be false. 
       </c:when>  
   </c:choose>  

   add <c:out value="${1+1}"/>

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
 
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
           <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     add <c:out value="${1+1}"/>

     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>
     <p> Testing c:set, c:if, c:forEach </p>
 
     <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
     <c:if test="${isTrue}">
         <c:forEach var="I" begin="1" end="2">  
             Item <c:out value="${I}"/>
             <%= 1+2 %>
             </br>  
         </c:forEach> 
     </c:if> 
 
     <p> Testing c:choose </p>
 

     <c:if test="${isTrue}">
        <%= 1+2 %>
        <%= 1+3 %>
        <%= 1+4 %>
        <%= new java.util.Date() %>
    </c:if> 

    add <c:out value="${1+1}"/>

    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>

    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 

    <p> Testing c:choose </p>

    add <c:out value="${1+1}"/>

    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>

    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 

    <p> Testing c:choose </p>


    <c:if test="${isTrue}">
       <%= 1+2 %>
       <%= 1+3 %>
       <%= 1+4 %>
       <%= new java.util.Date() %>
   </c:if> 

   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>

   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 

   <p> Testing c:choose </p>


   <c:if test="${isTrue}">
      <%= 1+2 %>
      <%= 1+3 %>
      <%= 1+4 %>
      <%= new java.util.Date() %>

      
  </c:if> 

  <%= 1+2 %>
  <%= 1+3 %>
  <%= 1+4 %>
  <%= new java.util.Date() %>
  <p> Testing c:set, c:if, c:forEach </p>

  <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

  <c:if test="${isTrue}">
      <c:forEach var="I" begin="1" end="2">  
          Item <c:out value="${I}"/>
          <%= 1+2 %>
          </br>  
      </c:forEach> 
  </c:if> 

  <p> Testing c:choose </p>


  <c:if test="${isTrue}">
     <%= 1+2 %>
     <%= 1+3 %>
     <%= 1+4 %>
     <%= new java.util.Date() %>

     
 </c:if> 

 <%= 1+2 %>
 <%= 1+3 %>
 <%= 1+4 %>
HELLO WORLD!


     <p> Testing c:choose </p>
 
     <c:choose>  
         <c:when test="${isTrue}">  
             c:when works!   
             <%= 1+2 %>
         </c:when>  
         <c:when test="${!isTrue}">  
             Should never be false. 
         </c:when>  
     </c:choose>  

     <c:if test="${isTrue}">
        <%= 1+2 %>
        <%= 1+3 %>
        <%= 1+4 %>
        <%= new java.util.Date() %>
    </c:if> 
 
    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>
 
    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 
 
    <p> Testing c:choose </p>
 
 
    <c:if test="${isTrue}">
       <%= 1+2 %>
       <%= 1+3 %>
       <%= 1+4 %>
       <%= new java.util.Date() %>
 
       
   </c:if> 
 
   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>
 
   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 
 
   <p> Testing c:choose </p>
 
 
   <c:if test="${isTrue}">
      <%= 1+2 %>
      <%= 1+3 %>
      <%= 1+4 %>
      <%= new java.util.Date() %>
 
      
  </c:if> 
 
  <%= 1+2 %>
  <%= 1+3 %>
  <%= 1+4 %>
 HELLO WORLD!
 
 
      <p> Testing c:choose </p>
  
      <c:choose>  
          <c:when test="${isTrue}">  
              c:when works!   
              <%= 1+2 %>
          </c:when>  
          <c:when test="${!isTrue}">  
              Should never be false. 
          </c:when>  
      </c:choose>  

      
      <c:if test="${isTrue}">
        <%= 1+2 %>
        <%= 1+3 %>
        <%= 1+4 %>
        <%= new java.util.Date() %>
    </c:if> 
 
    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>
 
    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 
 
    <p> Testing c:choose </p>
 
 
    <c:if test="${isTrue}">
       <%= 1+2 %>
       <%= 1+3 %>
       <%= 1+4 %>
       <%= new java.util.Date() %>
 
       
   </c:if> 
 
   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>
 
   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 
 
   <p> Testing c:choose </p>
 
 
   <c:if test="${isTrue}">
      <%= 1+2 %>
      <%= 1+3 %>
      <%= 1+4 %>
      <%= new java.util.Date() %>
 
      
  </c:if> 
 
  <%= 1+2 %>
  <%= 1+3 %>
  <%= 1+4 %>
 HELLO WORLD!
 
 
      <p> Testing c:choose </p>
  
      <c:choose>  
          <c:when test="${isTrue}">  
              c:when works!   
              <%= 1+2 %>
          </c:when>  
          <c:when test="${!isTrue}">  
              Should never be false. 
          </c:when>  
      </c:choose>  

      
      <c:if test="${isTrue}">
        <%= 1+2 %>
        <%= 1+3 %>
        <%= 1+4 %>
        <%= new java.util.Date() %>
    </c:if> 
 
    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>
 
    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 
 
    <p> Testing c:choose </p>
 
 
    <c:if test="${isTrue}">
       <%= 1+2 %>
       <%= 1+3 %>
       <%= 1+4 %>
       <%= new java.util.Date() %>
 
       
   </c:if> 
 
   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>
 
   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 
 
   <p> Testing c:choose </p>
 
 
   <c:if test="${isTrue}">
      <%= 1+2 %>
      <%= 1+3 %>
      <%= 1+4 %>
      <%= new java.util.Date() %>
 
      
  </c:if> 
 
  <%= 1+2 %>
  <%= 1+3 %>
  <%= 1+4 %>
 HELLO WORLD!
 
 
      <p> Testing c:choose </p>
  
      <c:choose>  
          <c:when test="${isTrue}">  
              c:when works!   
              <%= 1+2 %>
          </c:when>  
          <c:when test="${!isTrue}">  
              Should never be false. 
          </c:when>  
      </c:choose>  

      <c:if test="${isTrue}">
        <%= 1+2 %>
        <%= 1+3 %>
        <%= 1+4 %>
        <%= new java.util.Date() %>
    </c:if> 
 
    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>
 
    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 
 
    <p> Testing c:choose </p>
 
 
    <c:if test="${isTrue}">
       <%= 1+2 %>
       <%= 1+3 %>
       <%= 1+4 %>
       <%= new java.util.Date() %>
 
       
   </c:if> 
 
   <%= 1+2 %>
   <%= 1+3 %>
   <%= 1+4 %>
   <%= new java.util.Date() %>
   <p> Testing c:set, c:if, c:forEach </p>
 
   <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
   <c:if test="${isTrue}">
       <c:forEach var="I" begin="1" end="2">  
           Item <c:out value="${I}"/>
           <%= 1+2 %>
           </br>  
       </c:forEach> 
   </c:if> 
 
   <p> Testing c:choose </p>
 
 
   <c:if test="${isTrue}">
      <%= 1+2 %>
      <%= 1+3 %>
      <%= 1+4 %>
      <%= new java.util.Date() %>
 
      
  </c:if> 
 
  <%= 1+2 %>
  <%= 1+3 %>
  <%= 1+4 %>
 HELLO WORLD!
 
 
      <p> Testing c:choose </p>
  
      <c:choose>  
          <c:when test="${isTrue}">  
              c:when works!   
              <%= 1+2 %>
          </c:when>  
          <c:when test="${!isTrue}">  
              Should never be false. 
          </c:when>  
      </c:choose>  

      
      <c:if test="${isTrue}">
        <%= 1+2 %>
        <%= 1+3 %>
        <%= 1+4 %>
        <%= new java.util.Date() %>
    </c:if> 
 
    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>
    <p> Testing c:set, c:if, c:forEach </p>
 
    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  
 
    <c:if test="${isTrue}">
        <c:forEach var="I" begin="1" end="2">  
            Item <c:out value="${I}"/>
            <%= 1+2 %>
            </br>  
        </c:forEach> 
    </c:if> 
 
    <p> Testing c:choose </p>
 
 
    <c:if test="${isTrue}">
       <%= 1+2 %>
       <%= 1+3 %>
       <%= 1+4 %>
       <%= new java.util.Date() %>
 
       
   </c:if> 
 

   <c:if test="${isTrue}">
    <%= 1+2 %>
    <%= 1+3 %>
    <%= 1+4 %>
    <%= new java.util.Date() %>

    
</c:if> 

<%= new java.util.Date() %>
<%= new java.util.Date() %>
<%= new java.util.Date() %>
<%= 1+2 %>
<%= 1+3 %>
<%= 1+4 %>
<%= new java.util.Date() %>
<%= 1+2 %>
<%= 1+3 %>
<%= 1+4 %>
<%= new java.util.Date() %>
<%= 1+3 %>
 
 </body>
 </html>
