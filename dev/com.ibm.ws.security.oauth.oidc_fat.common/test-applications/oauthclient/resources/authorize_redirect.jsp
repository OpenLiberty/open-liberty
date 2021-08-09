<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=UTF-8" session="false" 
	pageEncoding="UTF-8"
	
	import="java.util.Map"
%>
<html>
<body>
Resp status: <%=response.getStatus() %>
<%

String code = request.getParameter("code");
%>
Headers: <%=response.getHeaderNames()%><br/>;
Received authorization code: <%=code%>.<br />
<%
response.setStatus(response.getStatus());
%>
</body>
</html>