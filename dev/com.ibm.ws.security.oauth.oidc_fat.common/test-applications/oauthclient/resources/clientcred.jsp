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
    import="java.util.Map"
    import="java.util.List"  
	import="com.ibm.json.java.JSONObject"

%>
<%
	// mode constants used to decide which part of the flow we are in
	final String MODE_RESET = "reset";
	final String MODE_PROCESS = "process";
	
	String mode = MODE_RESET;
	if ("true".equals(request.getParameter("auto"))) {
		mode = MODE_PROCESS;
	}

	// default values	
	String testName = "notSet" ;
	String clientId = "key";
	String clientSecret = "secret";
	String urlBase = "https://localhost:9443";
	String tokenEndpoint = urlBase + "/oauth/token.jsp";
	String resourceEndpoint = urlBase + "/oauth/resource.jsp";
	String scope = "scope1 scope2";
	String autoConsent = "false";
	
	// read values from the input parameters if mode is process
	if (MODE_PROCESS.equals(mode)) {
		testName = request.getParameter("test_name") ;
		clientId = request.getParameter("client_id");
		clientSecret = request.getParameter("client_secret");
		tokenEndpoint = request.getParameter("token_endpoint");
		resourceEndpoint = request.getParameter("resource_endpoint");
		scope = request.getParameter("scope");
		autoConsent= request.getParameter("autoauthz");
	} 
%>
<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>OAuth 2.0 Client Credentials Flow</title>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>OAuth 2.0 Client Credentials Flow</h1>
NOTE: The Client Credentials flow is used when the client is the resource owner. This flow is more like peer-to-peer than OAuth delegated
authroization. It relies on secure validation of the client ID and secret using the product's client table. The client
accesses the token endpoint with the client ID and secret to be exchanged for an access token for future resource requests. <br>
<br>
<form name="tokform" method="POST" action="clientcred.jsp">
<input type="hidden" name="auto" value="true" />
<table width=800>
<tr><td>Client Id</td><td><input type="text" name="client_id" value="<%=clientId%>" /></td></tr>
<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="<%=clientSecret%>" /></td></tr>
<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="<%=tokenEndpoint%>" size="60" /></td></tr>
<tr><td>Resource Endpoint</td><td><input type="text" name="resource_endpoint" value="<%=resourceEndpoint%>" size="60" /><br>
	<button type="button" onClick="tokform.elements['resource_endpoint'].value='<%=urlBase%>'+'/oauth/resource.jsp';">Set Default</button> &nbsp; 
	<button type="button" onClick="tokform.elements['resource_endpoint'].value='<%=urlBase%>'+'/oauth/sfresource.jsp';">Set Servlet Filter Default</button>
</td></tr>
<tr><td>Scope</td><td><input type="text" name="scope" value="<%=scope%>" /></td></tr>
<tr><td>Scope</td><td><input type="text" name="autoauthz" value="<%=autoConsent%>" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="submit" style="width:100%">Process Client Credentials Request</button></center></td></tr>
</table>
</form>

<%
System.out.println("clientcred.jsp: Start Test Case:  " + testName + "===============================");


	if (MODE_PROCESS.equals(mode)) {

		// send request
		URL urlToken = new URL(tokenEndpoint);
		HttpURLConnection connToken = (HttpURLConnection) urlToken.openConnection();
		System.out.println("clientcred.jsp: [tokenEndpoint] opened the connection to " + urlToken + ": " + connToken);
		connToken.setRequestMethod("POST");
		connToken.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
		connToken.setDoOutput(true);
		OutputStreamWriter wrToken = new OutputStreamWriter(connToken.getOutputStream());
		StringBuffer sb = new StringBuffer();
		sb.append("client_id=" + clientId + 
		         "&client_secret=" + clientSecret +
		         "&grant_type=client_credentials");
		if (scope != null && scope.trim().length() > 0) {
			sb.append("&scope=" + scope);
		} 
		sb.append("&autoauthz=" + autoConsent);
		wrToken.write(sb.toString());
		wrToken.flush();
		wrToken.close();
		
		// read response
		connToken.connect();
		System.out.println("clientcred.jsp: [tokenEndpoint] connected");
		InputStream streamToken = null;
		int responseCodeToken = connToken.getResponseCode();
		System.out.println("clientcred.jsp: [tokenEndpoint] got response code: " + responseCodeToken);
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
        
        Map<String, List<String>> map = connToken.getHeaderFields();
        Set<String> keys = map.keySet();
        StringBuffer sbHeader = new StringBuffer();
        for( String key : keys){
            sbHeader.append(key + "=");
            List<String> strings = map.get(key);
            for(String str : strings){
                sbHeader.append( "'" + str + "',");               
            }
        }
        String headers = sbHeader.toString();
   		
%>
<br/>
Received from token endpoint: <%=resultToken%>
<br/><br/>
Headers: <%=headers%>
<br/>
<%		
			// look for access token in result
			//JSONObject json = new JSONObject(resultToken);
			String accessToken = null;
			String refreshToken = null;
			if (responseCodeToken < 400) {
				JSONObject json = JSONObject.parse(resultToken);
				if (json.containsKey("access_token")) {
					accessToken = (String)json.get("access_token");
				}
			}
			if(accessToken != null) {
%>
<br/>
Access Token: <%=accessToken%><br>
<%	
			// get the protected resource with the access token using POST 
			URL urlResource = new URL(resourceEndpoint);
			HttpURLConnection connResource = (HttpURLConnection) urlResource.openConnection();
			System.out.println("clientcred.jsp: [resourceEndpoint] opened connection to " + urlResource + ": " + connResource);
			connResource.setRequestMethod("POST");
			connResource.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			connResource.setDoOutput(true);
			OutputStreamWriter wrResource = new OutputStreamWriter(connResource.getOutputStream());
			wrResource.write("access_token=" + accessToken);
			wrResource.flush();
			wrResource.close();
			
			// read response
			connResource.connect();
			System.out.println("clientcred.jsp: [resourceEndpoint] connected");
			InputStream streamResource = null;
			int responseCodeResource = connResource.getResponseCode();
			System.out.println("clientcred.jsp: [resourceEndpoint] got response code: " + responseCodeToken);
			if (responseCodeResource >= 200 && responseCodeResource < 400) {
				streamResource = connResource.getInputStream();
			} else {
				streamResource = connResource.getErrorStream();
			}
			final char[] bufferResource = new char[1024];
			StringBuffer sbResource = new StringBuffer();
			InputStreamReader srResource = new InputStreamReader(streamResource, "UTF-8");
	
			int readResource;
			do {
				readResource = srResource.read(bufferResource, 0, bufferResource.length);
				if (readResource > 0) {
					sbResource.append(bufferResource, 0, readResource);
				}
			} while (readResource >= 0);
			srResource.close();
	
			String resultResource = new String(sbResource.toString().trim());
%>
<br/>
Resource: <%=resultResource%>
System.out.println("clientcred.jsp: End Test Case:  " + testName + "===============================");


<%			
		} else {
%>
<br/>
Result from token endpoint does not appear to contain an access token. The returned status code was: <%=responseCodeToken%>
<%	
response.setStatus(responseCodeToken) ;

		}
		
	} 
%>
   
</body>
</html>