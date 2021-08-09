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
    import="javax.net.ssl.HttpsURLConnection"
	import="java.io.OutputStreamWriter"
	import="java.io.InputStream"
	import="java.io.InputStreamReader"
	import="com.ibm.json.java.JSONObject"
	import="java.util.Random"
	import="java.util.Set"
    import="java.util.Map"
    import="java.util.List"
    import="com.ibm.ws.security.oauth_oidc.fat.clientcert.ClientCert"
    import="javax.net.ssl.SSLSocketFactory"  
%>

<%!
// helper method for creating random state string
	String generateRandomState() {
		// random 20 character state
		StringBuffer sb = new StringBuffer();
		String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random r = new Random();
		for (int i = 0; i < 20; i++) {
			sb.append(chars.charAt(r.nextInt(chars.length())));
		}
		return sb.toString();
	} 
%>
<%
	// mode constants used to decide which part of the flow we are in
	final String MODE_RESET = "reset";
	final String MODE_AUTHORIZE = "authorize";
	final String MODE_TOKEN = "token";
	final String MODE_ERROR = "error";

	String mode = MODE_RESET;
	String code = request.getParameter("code");
	String param = request.getParameter("error");

	if ("true".equals(request.getParameter("auto"))) {
		mode = MODE_AUTHORIZE;
	}
	else if (code != null && code.length() > 0) {
		mode = MODE_TOKEN;
	}
	else if (param != null && param.length() > 0) {
		mode = MODE_ERROR;
	}

	String testName = "notSet";
	String username = "shane";
	String clientId = "key";
	String clientSecret = "secret";
	String urlBase = "https://localhost:9443";

	String redirectUri = urlBase + "/oauthclient/redirect.jsp";

	String authorizeEndpoint = urlBase + "/oauth/authorize.jsp";
	String tokenEndpoint = urlBase + "/oauth/token.jsp";
	String resourceEndpoint = urlBase + "/oauth/resource.jsp";
	String rtype = request.getParameter("response_type");
	if (rtype == null) {
		rtype = "code";
	}
	String nonce = request.getParameter("nonce");

	// generate random state
	String state = generateRandomState();

	String scope = "scope1 scope2";

	String formAction = "client.jsp";

	String autoConsent = "false";

	// read and save session state from the input parameters if mode is authorize
	if (MODE_AUTHORIZE.equals(mode)) {
		testName = request.getParameter("test_name");
		username = request.getParameter("user_name");
		clientId = request.getParameter("client_id");
		clientSecret = request.getParameter("client_secret");
		redirectUri = request.getParameter("redirect_uri");
		authorizeEndpoint = request.getParameter("authorize_endpoint");
		tokenEndpoint = request.getParameter("token_endpoint");
		resourceEndpoint = request.getParameter("resource_endpoint");
		state = request.getParameter("state");
		scope = request.getParameter("scope");
		autoConsent = request.getParameter("autoauthz");
		nonce = request.getParameter("nonce");

		session.setAttribute("user_name", username);
		session.setAttribute("client_id", clientId);
		session.setAttribute("client_secret", clientSecret);
		session.setAttribute("redirect_uri", redirectUri);
		session.setAttribute("authorize_endpoint", authorizeEndpoint);
		session.setAttribute("token_endpoint", tokenEndpoint);
		session.setAttribute("resource_endpoint", resourceEndpoint);
		session.setAttribute("state", state);
		session.setAttribute("scope", scope);
		session.setAttribute("autoauthz", autoConsent);
		if (nonce != null && !nonce.isEmpty()) {
			session.setAttribute("nonce", nonce);
		}

		formAction = authorizeEndpoint;
	} else if (!MODE_RESET.equals(mode)) {
		// for all other cases than reset, restore session params
		testName = request.getParameter("test_name");
		username = (String) session.getAttribute("user_name");
		clientId = (String) session.getAttribute("client_id");
		clientSecret = (String) session.getAttribute("client_secret");
		redirectUri = (String) session.getAttribute("redirect_uri");
		authorizeEndpoint = (String) session.getAttribute("authorize_endpoint");
		tokenEndpoint = (String) session.getAttribute("token_endpoint");
		resourceEndpoint = (String) session.getAttribute("resource_endpoint");
		state = (String) session.getAttribute("state");
		scope = (String) session.getAttribute("scope");
		autoConsent = (String) session.getAttribute("autoauthz");
		if (nonce != null && !nonce.isEmpty()) {
			session.setAttribute("nonce", nonce);
		}
	}

	boolean validResponseState = false;
	String origState = state;
	String responseState = request.getParameter("state");
	if (MODE_TOKEN.equals(mode)) {
		// validate state at this point also
		if (origState != null && responseState != null && origState.equals(responseState)) {
			validResponseState = true;
			// generate new random state for next request
			state = generateRandomState();
		}
	}
%>

<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>OAuth 2.0 Authorization Code Grant</title>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>OAuth 2.0 Authorization Code Grant</h1>
<form name="authform" id="authform" method="GET" action="<%=formAction%>">
<input type="hidden" name="auto" value="true" />
<input name="response_type" value="<%=rtype%>" />
<table width=800>
<tr><td>TestName</td><td><input type="text" name="test_name" value="<%=testName%>" /></td></tr>
<tr><td>Username</td><td><input type="text" name="user_name" value="<%=username%>" /></td></tr>
<tr><td>Client Id</td><td><input type="text" name="client_id" value="<%=clientId%>" /></td></tr>
<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="<%=clientSecret%>" /></td></tr>
<tr><td>Redirect URI</td><td><input type="text" name="redirect_uri" value="<%=redirectUri%>" size="60" /></td></tr>
<tr><td>Authorize Endpoint</td><td><input type="text" name="authorize_endpoint" value="<%=authorizeEndpoint%>" size="60" /></td></tr>
<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="<%=tokenEndpoint%>" size="60" /></td></tr>
<tr><td>Resource Endpoint</td><td><input type="text" name="resource_endpoint" value="<%=resourceEndpoint%>" size="60" /><br>
	<button type="button" onClick="authform.elements['resource_endpoint'].value='<%=urlBase%>'+'/oauth/resource.jsp';">Set Default</button> &nbsp; 
	<button type="button" onClick="authform.elements['resource_endpoint'].value='<%=urlBase%>'+'/oauth/sfresource.jsp';">Set Servlet Filter Default</button>
</td></tr>
<tr><td>State</td><td><input type="text" name="state" size="30" value="<%=state%>" /></td></tr>
<tr><td>Scope</td><td><input type="text" name="scope" size="30" value="<%=scope%>" /></td></tr>
<tr><td>autoConsent</td><td><input type="text" name="autoauthz" size="30" value="<%=autoConsent%>" /></td></tr>
<tr><td>Nonce</td><td><input type="text" name="nonce" size="30" value="<%=nonce%>" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="processAzn" style="width:100%">Process Authorization</button></center></td></tr>
</table>  
</form>
<%
	System.out.println("client.jsp: Start Test Case:  " + testName + "===============================");
	if (mode.equals(MODE_AUTHORIZE)) {
		// auto-post the form
%>
<script type="text/javascript">
	setTimeout('document.getElementById("authform").submit()', 0);
</script>
<%
	}
	else if (mode.equals(MODE_ERROR)) {
		String errorResult = request.getParameter("error");
		String errorDesc = request.getParameter("error_description");
%>
<b><font COLOR="red">Received error</font></b><br>
Error type: <%=errorResult%><br>
Error Description: <%=errorDesc%><br>
<%
	} else if (mode.equals(MODE_TOKEN)) {
%>
Received authorization code: <%=code%>.<br />
<%
	// display state validation result
		if (!validResponseState) {
%>
Error: state mismatch. Original state: <%=origState%> Response state: <%=responseState%><br />
<%
	} else {
			// proceed with exchanging authorization code for access token
%>
State validated ok. Original state: <%=origState%> Response state: <%=responseState%><br />
Exchanging for token....
<%
	// send request
			URL urlToken = new URL(tokenEndpoint);
			HttpURLConnection connToken = (HttpURLConnection) urlToken.openConnection();
			SSLSocketFactory sf = ClientCert.getClientCertSocketFactory("commonBasicKeyStore.jks", "Liberty", "jks");
			((HttpsURLConnection) connToken).setSSLSocketFactory(sf);
			System.out.println("client.jsp: [tokenEndpoint] opened the connection to " + urlToken + ": " + connToken);
			connToken.setRequestMethod("POST");
			connToken.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			connToken.setDoOutput(true);
			OutputStreamWriter wrToken = new OutputStreamWriter(connToken.getOutputStream());
			String requestString = "client_id=" + clientId + 
			         "&client_secret=" + clientSecret +
			         "&grant_type=authorization_code" +
			         "&redirect_uri=" + redirectUri + 
			         "&code=" + code ;
			// enable the following line for debugging, leaving it enabled will cause the password checking tests to fail
			//System.out.println("client.jsp:  request string: " + requestString);
			wrToken.write(requestString);
			wrToken.flush();
			wrToken.close();

			// read response
			connToken.connect();
			System.out.println("client.jsp:  [tokenEndpoint] connected");
			InputStream streamToken = null;
			int responseCodeToken = connToken.getResponseCode();
			System.out.println("client.jsp:  [tokenEndpoint] got response code: " + responseCodeToken);
			if (responseCodeToken >= 200 && responseCodeToken < 400) {
				streamToken = connToken.getInputStream();
			} else {
				streamToken = connToken.getErrorStream();
			}
			final char[] bufferToken = new char[1024];
			StringBuffer sbToken = new StringBuffer();
	        if (streamToken != null) {
				InputStreamReader srToken = new InputStreamReader(streamToken, "UTF-8");

				int readToken;
				do {
					readToken = srToken.read(bufferToken, 0, bufferToken.length);
					if (readToken > 0) {
						sbToken.append(bufferToken, 0, readToken);
					}
				} while (readToken >= 0);
				srToken.close();
            }

			String resultToken = new String(sbToken.toString().trim());

			Map<String, List<String>> map = connToken.getHeaderFields();
			Set<String> keys = map.keySet();
			StringBuffer sbHeader = new StringBuffer();
			for (String key : keys) {
				sbHeader.append(key + "=");
				List<String> strings = map.get(key);
				for (String str : strings) {
					sbHeader.append("'" + str + "',");
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
					accessToken = (String) json.get("access_token");
				}
				if (json.containsKey("refresh_token")) {
					refreshToken = (String) json.get("refresh_token");
				}
			}
			if (accessToken != null) {
%>
<br/>
Access Token: <%=accessToken%><br>
Refresh Token: <%=refreshToken%>
				
<br/>
		
System.out.println("client.jsp: End Test Case:  " + testName + "===============================");
<%
					response.setStatus(responseCodeToken);
							} else {
				%>
<br/>
Result from token endpoint does not appear to contain an access token. The returned status code was: <%=responseCodeToken%>
<%
	response.setStatus(responseCodeToken);

			}
		}
	} // else if (mode.equals(MODE_TOKEN)) {
%>
</body>
</html>
