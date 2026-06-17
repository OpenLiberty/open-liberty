<!--
    Copyright (c) 2026 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Test Empty Include Path with Flush</title>
</head>
<body>
    <h1>Testing Empty Include Path with flush="true"</h1>
    <p>This JSP attempts to include an empty string path with flush="true", which should throw a ServletException (not NPE).</p>
    
    <%
    // Set the include path to an empty string - exactly as shown in the issue
    String includePage01 = "";
    %>
    
    <!-- This is the exact scenario from the issue report -->
    <jsp:include page="<%= includePage01 %>" flush="true"/>
    
    <p>If you see this, the include did not throw an exception (unexpected).</p>
</body>
</html>
