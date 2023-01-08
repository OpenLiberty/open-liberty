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
 <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>  
 <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>   
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Format Tags</title>
</head>
<body>
    <fmt:setLocale value = "en_US"/>
    <p> Testing fmt:parseDate, fmt:formatDate </p>
    <!-- Using Current Date of System!-->
    
    <c:set var="date" value="<%=new java.util.Date()%>" />

    Formatted Date: <fmt:formatDate dateStyle="MEDIUM" value="${date}" />

    <!-- Using Jan 1 2000 for Parsing !--> 
    <c:set var="dateToParse" value="01-01-2000" />

    <fmt:parseDate var="parsedDate" value="${dateToParse}" pattern="MM-dd-yyyy" />

    Parsed date: <c:out value="${parsedDate}" />
  
</body>
</html>
