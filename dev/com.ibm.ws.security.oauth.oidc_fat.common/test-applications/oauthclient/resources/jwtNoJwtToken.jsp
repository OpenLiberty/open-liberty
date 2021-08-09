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
    //String clientId = "key";
    //String clientSecret = "secret";
    //String jwtBearerToken = "ENTER HERE";
    String testName = null ;
    String clientId = null ;
    String clientSecret = null ;
    String urlBase = "https://localhost:9443";
    String tokenEndpoint = urlBase + "/oidc/endpoint/OAuthConfigSample/token";
    String jwt_scope = "openid profile";
    String extraParams = "";

    
    if ("true".equals(request.getParameter("auto"))) {
        System.out.println("jwtNoJwtToken.jsp: in auto");
        submit = true;
        testName = request.getParameter("testCaseName");
        clientId = request.getParameter("client_id");
        clientSecret = request.getParameter("client_secret");
        tokenEndpoint = request.getParameter("token_endpoint");
        System.out.println("jwtNoJwtToken.jsp: client_id: " + clientId);
        System.out.println("jwtNoJwtToken.jsp: client_secret: " + clientSecret);
        System.out.println("jwtNoJwtToken.jsp: token_endpoint: " + tokenEndpoint);
        jwt_scope = request.getParameter("scope"); 
        System.out.println("jwt.jsp: scope: " + jwt_scope);
        if( jwt_scope != null && (!jwt_scope.isEmpty()) ){
            extraParams = extraParams + "&scope=" + jwt_scope;  
        }

    }
%>
<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>OAuth 2.0 jwt-bearer Token Request</title>
</head>
<body onload="javascript:processAccessToken();">
<%@ include file="header.jsp"%>
<h1>OAuth 2.0 Jwt-BVearer Token Request</h1>
<form name="tokform" method="POST" action="jwtNoJwtToken.jsp">
<input type="hidden" name="auto" value="true" />
<table width=800>
<tr><td>Client Id</td><td><input type="text" name="testCaseName" value="<%=testName%>" /></td></tr>
<tr><td>Client Id</td><td><input type="text" name="client_id" value="<%=clientId%>" /></td></tr>
<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="<%=clientSecret%>" /></td></tr>
<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="<%=tokenEndpoint%>" size="60" /></td></tr>
<tr><td>JWT Scopes</td><td><input type="text" name="scope" value="<%=jwt_scope%>" size="60" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="submit" style="width:100%">Process JWT Bearer Request</button></center></td></tr>
</table>
</form>


<%

if (submit) {

        // send request
        URL urlToken = new URL(tokenEndpoint);
        HttpURLConnection connToken = (HttpURLConnection) urlToken.openConnection();
        System.out.println("jwtNoJwtToken.jsp: Start Test Case:  " + testName + "===============================");
        System.out.println("jwtNoJwtToken.jsp: opened the connection to " + urlToken + ": " + connToken);
        connToken.setRequestMethod("POST");
        connToken.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        connToken.setDoOutput(true);
        OutputStreamWriter wrToken = new OutputStreamWriter(connToken.getOutputStream());
        StringBuffer sb = new StringBuffer();
        sb.append("client_id=" + clientId + 
                 "&client_secret=" + clientSecret +
                 "&grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" +
                  extraParams);
        wrToken.write(sb.toString());
        wrToken.flush();
        wrToken.close();
        
        // read response
        connToken.connect();
        System.out.println("jwtNoJwtToken.jsp: string buffer: " + sb.toString());
        System.out.println("jwtNoJwtToken.jsp: connected");
        InputStream streamToken = null;
        int responseCodeToken = connToken.getResponseCode();
        System.out.println("jwtNoJwtToken.jsp: got response code: " + responseCodeToken);
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
            json = JSONObject.parse(resultToken);
            keySet = (Set<String>) json.keySet();
        } catch (Exception jsone) {
            // fall through to no token
        }
        if (keySet != null && keySet.contains("access_token")) {
            String refreshTokenNew = (String)"none";
            String idTokenNew = (String)"none";
            String accessToken = (String)json.get("access_token");;
            if(keySet.contains("refresh_token" )){
                refreshTokenNew = (String)json.get("refresh_token");
            };
            if(keySet.contains("id_token" )){
                idTokenNew = (String)json.get("id_token");
            };

%>
<br/>
<BR>
ResultToken: <%=resultToken%><BR><BR>
Access Token: <%=accessToken%><br>
Refresh Token: <%=refreshTokenNew%><br>
ID Token: <%=idTokenNew%><BR>      
<br>
Updated "Refresh Token" input field with: <%=refreshTokenNew%>
<%            
        } else {
%>
<br/>
Response did not contain access token.
<%      
response.setStatus(responseCodeToken) ;
System.out.println("jwtNoJwtToken.jsp: End Test Case:  " + testName + "===============================");
        }   
} // end submit     

%>
</body>
</html>
