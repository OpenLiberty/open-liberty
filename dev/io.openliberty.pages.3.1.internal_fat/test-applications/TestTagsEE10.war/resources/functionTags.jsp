<!--
    Copyright (c) 2022 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
 <%@ taglib uri="jakarta.tags.core" prefix="c" %>  
 <%@ taglib uri="jakarta.tags.functions" prefix="fn" %>  
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
