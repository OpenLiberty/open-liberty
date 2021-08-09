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

<%
    String urlIdpRequest = "https://localhost:9443/sps/WlpTfimIdp1/saml20/logininitial";
    String urlBase = "http://localhost:8010/";
    String urlSslBase = "https://localhost:8020/";
    String urlPartnerId = urlSslBase + "ibm/saml20/sp/acs";
    String nameIdFormat = "email";
    String urlTarget = urlSslBase + "samlclient/fat/snoop";
    String testCaseName = "testCaseName-1";

    
    String formAction = "processIdp.jsp";
    
%>

<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>Saml 2.0 - Request SAML IdP</title>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>SAML 2.0 - Request SAML IdP</h1>
<form name="idpform" id="idpform" method="GET" action="<%=formAction%>">
<input type="hidden" name="auto" value="true" />
<table width=800>
<tr><td>Test Case Name</td><td><input type="text" name="testCaseName" value="<%=testCaseName%>" /></td></tr>
<tr><td>IdP Request URL</td><td><input type="text" name="urlIdpRequest" value="<%=urlIdpRequest%>" size="90"/></td></tr>
<tr><td>ACS Endpoint</td><td><input type="text" name="urlPartnerId" value="<%=urlPartnerId%>"  size="60" /></td></tr>
<tr><td>Name ID Format</td><td><input type="text" name="nameIdFormat" value="<%=nameIdFormat%>" /></td></tr>
<tr><td>Protected Reqource URL</td><td><input type="text" name="urlTarget" value="<%=urlTarget%>" size="60" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="processIdp" style="width:100%">Process Idp Request</button></center></td></tr>
</table>  
</form>
</body>
</html>
