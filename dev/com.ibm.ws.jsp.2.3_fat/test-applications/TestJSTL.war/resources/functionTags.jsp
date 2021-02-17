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
 <%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>  
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Function Tags</title>
</head>
<body>

    <p> Testing fn:contains, fn:length </p>

    <c:set var="string" value="Function Tags Page"/>  
  
    <c:if test="${fn:contains(string, 'Tags')}">  
        <p>Function Tag Works!<p>  
        <p>Length of 'Tags' is: ${fn:length("Tags")}<p>  
    </c:if>  

</body>
</html>
