<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"
	import="com.ibm.ws.security.openidconnect.common.Constants"
	import="java.net.URL"
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
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>OpenID Connect Session Management Page</title>
</head>
<%
String expectedRpOrigin = request.getHeader("referer");
if (expectedRpOrigin != null) {
   	URL referrerUrl = new URL(expectedRpOrigin);
   	String path = referrerUrl.getPath();
   	if (path != null && !path.isEmpty()) {
   		expectedRpOrigin = expectedRpOrigin.substring(0, expectedRpOrigin.indexOf(path));
   	}
}
%>
<script src="scripts/sha256.js"></script>
<script src="scripts/enc-base64-min.js"></script>
<script src="scripts/opiframe.js"></script>
<script>
var EXPECTED_ORIGIN = '<%= expectedRpOrigin %>';
var BROWSER_STATE_COOKIE_NAME = '<%= Constants.BROWSER_STATE_COOKIE %>';
</script>
<body>
OpenID Connect Provider session management page<br/>
</body>
</html>