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

    <c:set var="date" value="02-17-2021" />  
    <fmt:parseDate value="${date}" var="parsedDate"  pattern="dd-MM-yyyy" />  
    <p>
        <c:out value="${parsedDate}" />
    </p>  
    <fmt:formatDate type="both" dateStyle="long" timeStyle="long"  value="${date}" />
    
</body>
</html>