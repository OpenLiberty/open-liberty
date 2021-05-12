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
	
	import="java.net.URL"
	import="java.net.HttpURLConnection"
	import="java.io.OutputStreamWriter"
	import="java.io.InputStream"
	import="java.io.InputStreamReader"
	import="java.util.Set"
	import="com.ibm.json.java.JSONObject"
%>
<%
	boolean submit = false;
	String clientId = "key";
	String clientSecret = "secret";
	String refreshToken = "ENTER HERE";
	String urlBase = "https://localhost:9443";
	String tokenEndpoint = urlBase + "/oauth/token.jsp";
	String scope = "scope1 scope2";
	
	if ("true".equals(request.getParameter("auto"))) {
		submit = true;
		clientId = request.getParameter("client_id");
		clientSecret = request.getParameter("client_secret");
		refreshToken = request.getParameter("refresh_token");
		tokenEndpoint = request.getParameter("token_endpoint");
		scope = request.getParameter("scope");
	}
%>
<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>OAuth 2.0 Refresh Token Request</title>
</head>
<body onload="javascript:processAccessToken();">
<%@ include file="header.jsp"%>
<h1>OAuth 2.0 Refresh Token Request</h1>
<form name="tokform" method="POST" action="refresh.jsp">
<input type="hidden" name="auto" value="true" />
<table width=800>
<tr><td>Client Id</td><td><input type="text" name="client_id" value="<%=clientId%>" /></td></tr>
<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="<%=clientSecret%>" /></td></tr>
<tr><td>Refresh Token</td><td><input type="text" name="refresh_token" value="<%=refreshToken%>" size="60" /></td></tr>
<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="<%=tokenEndpoint%>" size="60" /></td></tr>
<tr><td>Scope</td><td><input type="text" name="scope" value="<%=scope%>" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="submit" style="width:100%">Process Refresh Request</button></center></td></tr>
</table>
</form>


<%
                                               
if (submit) {

		// send request
		URL urlToken = new URL(tokenEndpoint);
		HttpURLConnection connToken = (HttpURLConnection) urlToken.openConnection();
		System.out.println("refresh.jsp: opened the connection to " + urlToken + ": " + connToken);
		connToken.setRequestMethod("POST");
		connToken.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
		connToken.setDoOutput(true);
		OutputStreamWriter wrToken = new OutputStreamWriter(connToken.getOutputStream());
		StringBuffer sb = new StringBuffer();
		sb.append("client_id=" + clientId + 
		         "&client_secret=" + clientSecret +
		         "&grant_type=refresh_token" +
		         "&refresh_token=" + refreshToken);
		if (scope != null && scope.trim().length() > 0) {
			sb.append("&scope=" + scope);
		} 
		wrToken.write(sb.toString());
		wrToken.flush();
		wrToken.close();
		
		// read response
		connToken.connect();
		System.out.println("refresh.jsp: connected");
		InputStream streamToken = null;
		int responseCodeToken = connToken.getResponseCode();
		System.out.println("refresh.jsp: got response code: " + responseCodeToken);
		if (responseCodeToken >= 200 && responseCodeToken < 400) {
			streamToken = connToken.getInputStream();
		} else {
			streamToken = connToken.getErrorStream();
		}
		final char[] bufferToken = new char[1024];
		StringBuffer sbToken = new StringBuffer();
		InputStreamReader srToken = new InputStreamReader(streamToken, "UTF-8");

		int readToken;
		do {
			readToken = srToken.read(bufferToken, 0, bufferToken.length);
			if (readToken > 0) {
				sbToken.append(bufferToken, 0, readToken);
			}
		} while (readToken >= 0);
		srToken.close();

		String resultToken = new String(sbToken.toString().trim());
%>
<br/>
Received from token endpoint: <%=resultToken%>
<%			  
		// look for access token in result or an error
		Set<String> keySet = null;
		JSONObject json = null;
		try {
			//json = new JSONObject(resultToken);
			json = JSONObject.parse(resultToken);
			keySet = (Set<String>) json.keySet();
		} catch (Exception jsone) {
			// fall through to no token
		}
		if (keySet != null && keySet.contains("access_token")) {
			String accessToken = (String)json.get("access_token");
			String refreshTokenNew = (String)json.get("refresh_token");
%>
<br/>
Access Token: <%=accessToken%><br>
Refresh Token: <%=refreshTokenNew%><br>
<br>
<script language="javascript">
	tokform.elements['refresh_token'].value = '<%=refreshTokenNew%>';
</script>
Updated "Refresh Token" input field with: <%=refreshTokenNew%>
      
<%			  
		} else {
%>
<br/>
Response did not contain access token.
<%		
response.setStatus(responseCodeToken) ;

		} 	
} // end submit 	

%>
</body>
</html>
