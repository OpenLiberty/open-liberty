<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=UTF-8"

	pageEncoding="UTF-8"
	
%>

<title>User Login Test Page</title>
</head>
<body style="margin:10px; padding:10px">
<h2>User Login Test Servlet</h2>

<br>
<br>Description: this is a tiny sample servlet that prints the authenticed user principal and invokes the PropagationHelper.getUserInfo API method.
<br>PropagationHelper is only present in oidc clients, but this is safe to package in other environments (like Social)
<br>because this jsp won't get compiled unless it's invoked.
<br>

<%
	Object o = request.getUserPrincipal();
	if (o == null) {
		%>user principal is null<br><%
	} else {
		%>And the current user is:<h2><pre>   <%=o.toString()%></pre></h2><%
	}
	
    String userInfo = null;
   
    try{
        Class.forName("com.ibm.websphere.security.openidconnect.PropagationHelper");
        userInfo = com.ibm.websphere.security.openidconnect.PropagationHelper.getUserInfo();
                
    } catch (ClassNotFoundException cnex){
        %>PropagationHelper class not found<br><%
    }
    
    if(userInfo == null){
    	%>user info from PropagationHelper is null<br><%
	} else {
		%>user info from PropagationHelper is: <%=userInfo%> <%
	}



%>

</body>
</html>