<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"
	import="com.ibm.ws.security.openidconnect.clients.common.Constants"
	import="java.net.URL"
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--
    Copyright (c) 2019, 2025 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
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
    String protocol = referrerUrl.getProtocol();
    String host = referrerUrl.getHost();
    int port = referrerUrl.getPort();

    expectedRpOrigin = protocol + "://" + host;
    if (port != -1) {
        expectedRpOrigin = expectedRpOrigin + ":" + port;
    }
}
%>
<script src="scripts/opiframe.js"></script>
<script>
var EXPECTED_ORIGIN = '<%= expectedRpOrigin %>';
var BROWSER_STATE_COOKIE_NAME = '<%= Constants.BROWSER_STATE_COOKIE %>';
</script>
<body>
OpenID Connect Provider session management page<br/>
</body>
</html>