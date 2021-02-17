<!--
    Copyright (c) 2021 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
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

    <p> Testing fmt:parseDate, fmt:formatDate </p>

    <fmt:setLocale value = "en_US"/>
    <c:set var="timeZone" value="GMT-5"/>

    <c:set var="date" value="02-17-2021" />  
    <fmt:parseDate value="${date}" var="parsedDate"  pattern="MM-dd-yyyy"/>  
    <fmt:timeZone value="${timeZone}">
        <fmt:formatDate dateStyle="medium" value="${parsedDate}" />
    </fmt:timeZone>
    
</body>
</html>
