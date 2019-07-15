<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
	import="java.util.*"
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--
    Copyright (c) 2019 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
-->
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>OAuth authorization form</title>
<script language="javascript">
function init() {
	var scope = oauthFormData.scope;
	var scopeEle = document.getElementById("oauth_scope");
	var ul = document.createElement("ul");
	if(scope) {
		for(var i=0; i < scope.length; i++) {
			var n = document.createElement("li");
			n.innerHTML = scope[i];
			ul.appendChild(n);
		}
	}
	scopeEle.appendChild(ul);
	// set client name
	var clientEle = document.getElementById("client_name");
	clientEle.innerHTML = oauthFormData.clientDisplayName;
}
</script>
</head>
<%
	Locale locale = request.getLocale();
	ResourceBundle rb = ResourceBundle.getBundle("template", locale);
%>
<body onload="init()">
	<div><%=String.format(rb.getString("template.confirmation"), "<span id=client_name style=\"font-weight:bold\">xxxxxxx</span>")%></div>
	<div id="oauth_scope">
	</div>
	<div><input type="button" value="<%=rb.getString("template.authorize")%>" onclick="javascript:submitForm(oauthFormData);"/>
	<input type="button" value="<%=rb.getString("template.deny")%>"/></div>
</body>
</html>

