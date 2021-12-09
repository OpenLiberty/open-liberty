<!--
    Copyright (c) 2021 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<title>Core Tags</title>
</head>
<body>

    <p> Testing c:set, c:if, c:forEach </p>

    <c:set var="isTrue" scope="session" value="${Boolean.TRUE}"/>  

    <c:if test="${isTrue}">
        <c:forEach var="i" begin="1" end="2">  
            Item <c:out value="${i}"/>
            </br>  
        </c:forEach> 
    </c:if> 

    <p> Testing c:choose </p>

    <c:choose>  
        <c:when test="${isTrue}">  
            c:when works!   
        </c:when>  
        <c:when test="${!isTrue}">  
            Should never be false. 
        </c:when>  
    </c:choose>  
    
</body>
</html>
