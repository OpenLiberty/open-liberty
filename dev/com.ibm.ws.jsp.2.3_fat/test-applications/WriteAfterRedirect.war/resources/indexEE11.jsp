<!--
    Copyright (c) 2025 IBM Corporation and others.
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

<title>Index EE11 and Later</title>
</head>
<body>
     
    <!-- Special Scenario FOR EE11 to avoid  SRVE0199E: OutputStream already obtained error.-->
    <!-- 
        EE11's sendRedirect(String target) clears the buffer, but previously it did not. 
        See https://github.com/OpenLiberty/open-liberty/issues/30747
    -->

    <%
    String targetUrl = "page2.jsp";
    response.sendRedirect(targetUrl, false);
    System.out.println("DEBUG: sendRedirect END");
    %>

    <jsp:include page="footer.jsp" />
    
</body>
</html>
