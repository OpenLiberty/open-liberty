<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<title>OAuth Component Sample Client</title>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>OAuth 2.0 Test Client</h1>
This is the OAuth component sample client. To view or edit the server configuration settings, use index.jsp from the server's com.ibm.oauth.test application.<br>
<br>
<table width=800>
<tr><td><a href="client.jsp">Authorization Code</a></td><td>3-legged OAuth flow with authorization grant, access token and resource request.</td></tr>
<tr><td><a href="implicit.jsp">Implicit</a></td><td>2-legged OAuth flow for access token and resource request.</td></tr>
<tr><td><a href="refresh.jsp">Refresh Token</a></td><td>Use a refresh token to request an updated access token.</td></tr>
<tr><td><a href="clientcred.jsp">Client Credentials</a></td><td>Used when the client is the resource owner.</td></tr>
<tr><td><a href="resourceowner.jsp">Owner Password</a></td><td>Flow for environments needing separate validation.</td></tr>
<tr><td><a href="curl.jsp">Command-Line Using Curl</a></td><td>Interactive command line to view the OAuth requests and responses.</td></tr>
</table>

</body>
