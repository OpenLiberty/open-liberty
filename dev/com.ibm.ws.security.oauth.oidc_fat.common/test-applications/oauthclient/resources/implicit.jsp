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
String urlBase = "https://localhost:9443"; 
%>
<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<script language="javascript">

// read global vars from cookie (i.e. state), or initialize if first time
var username = getSessionCookie("user_name");
if (username == null) username = "shane"; 
var clientId = getSessionCookie("client_id");
if (clientId == null) clientId = "key";
var redirectUri = getSessionCookie("redirect_uri");
if (redirectUri == null) redirectUri = "<%=urlBase%>" + "/oauthclient/redirect.jsp";
var authorizeEndpoint = getSessionCookie("authorize_endpoint");
if (authorizeEndpoint == null) authorizeEndpoint = "<%=urlBase%>" + "/oauth/authorize.jsp";
var resourceEndpoint = getSessionCookie("resource_endpoint");
if (resourceEndpoint == null) resourceEndpoint = "<%=urlBase%>" + "/oauth/resource.jsp";
var state = getSessionCookie("state");
if (state == null) state = generateRandomState();
var scope = getSessionCookie("scope");
if (scope == null) scope = "scope1 scope2";
var autoauthz=getSessionCookie("autoauthz");
if (autoauthz == null) autoauthz = "false";
var oidc_prompt=getSessionCookie("prompt");
if (oidc_prompt == null) oidc_prompt = "login consent";
var oidc10_nonce = getSessionCookie("nonce");
if (oidc10_nonce == null) oidc10_nonce = "default_nonce";
var response_type = getSessionCookie("response_type");
if (response_type == null) response_type = "id_token token";

function generateRandomState() {
	// generates a 20-byte random string of alpha-numerics
	var chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	var result = "";
	for (var i = 0; i < 20; i++) {
		result = result + chars.charAt(Math.floor(Math.random()*chars.length));
	}
	return result;	
}

function setSessionCookie(name, value) {
	// if using ssl like you should, use secure cookies
	if (document.URL.indexOf('https://') == 0) {
		document.cookie=name + "=" + escape(value) + "; secure";
	} else {
		document.cookie=name + "=" + escape(value);
	}
}

function getSessionCookie(name) {
	var cookies = document.cookie.split( ';' );
	var aCookie = '';
	var cname = '';
	var cvalue = null;

	// iterate over all cookies until we find a value, or check them all
	for ( var i = 0; ((i < cookies.length) && (cvalue == null)); i++ ) {
		// now we'll split apart each name=value pair
		aCookie = cookies[i].split( '=' );

		// grab the cookie name (trimmed)
		cname = aCookie[0].replace(/^\s+|\s+$/g, '');

		// if it's our cookie
		if ( cname == name ) {
			// if cookie has a value, get it (also trimmed)
			if ( aCookie.length > 1 ) {
				cvalue = unescape( aCookie[1].replace(/^\s+|\s+$/g, '') );
			}
		}
		aCookie = null;
		cname = '';
	}
	return cvalue;
}

function processAccessToken() {
	var response = getResponse();
	var error = response.error;
	if (error) {
		div = document.createElement("div");
		div.innerHTML = "<b><font COLOR='red'>Received error</font></b><br>Error type: "+error+"<br>Error Description: "+response.error_description+"<br>";
		document.body.appendChild(div);
		return;
	}
	var accessToken = response.access_token;
	if(!accessToken) {
		return;
	}
	var IdToken = response.id_token;
	
	// validate state
	var responseState = response.state;
   	if (state == null || responseState == null || !(state == responseState)) {
   		//perhaps a CSRF, in any case reject the whole fragment
		div = document.createElement("div");
		div.innerHTML = "Error validating state. Original: " + state + " State: " + state;
		document.body.appendChild(div);
   		return;
   }
   // generate new state for next request
   state = generateRandomState();
   var theForm = document.getElementById("authform");
   theForm.elements["state"].value = state;
   
	div = document.createElement("div");
	div.innerHTML = "Access Token : " + accessToken  + "<br/>" + "State ok: " + responseState;
	if (IdToken != null) {
		div.innerHTML = div.innerHTML + "<br/>id_token" + IdToken;
	}
	document.body.appendChild(div);
	
	var xhr;
	if (window.XMLHttpRequest) {
		xhr = new XMLHttpRequest();
	} else {
		xhr = new ActiveXObject("Microsoft.XMLHTTP");
	}
	xhr.onreadystatechange=function() {
		if (xhr.readyState == 4) {
			if(xhr.status == 200) {
				div = document.createElement("div");
				div.innerHTML = "Resource response:<br/>" + xhr.responseText;
		    	document.body.appendChild(div);
			} else {
				div = document.createElement("div");
				div.innerHTML = xhr.statusText + " : " + xhr.responseText;
		    	document.body.appendChild(div);
			}
	    }
	};
	xhr.open("POST",resourceEndpoint,true);
	xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");
	xhr.setRequestHeader("Authorization","Bearer " + accessToken);
	xhr.send(null);
	// or we can use parameter style to pass the access token
	// xhr.send("access_token=" + accessToken);
}

function getResponse() {
	var hash = window.location.hash;
	var retVal = {};
	if(hash && hash.charAt(0) === "#") {
		var parameters = hash.substr(1).split("&");
		for(var i = parameters.length; i--;) {
			var p = parameters[i];
			var pos = p.indexOf("=");
			if(pos != -1) {
				retVal[p.substr(0,pos)] = p.substr(pos+1);
			} else {
				retVal[p] = null;
			}
		}
	}
	return retVal;
}

function processTokenRequest() {
	// store all our form variables as cookie vals so we remember them
	var theForm = document.getElementById("authform");
	setSessionCookie("user_name",theForm.elements["user_name"].value);
	setSessionCookie("client_id",theForm.elements["client_id"].value);
	setSessionCookie("redirect_uri",theForm.elements["redirect_uri"].value);
	setSessionCookie("authorize_endpoint",theForm.elements["authorize_endpoint"].value);
	setSessionCookie("resource_endpoint",theForm.elements["resource_endpoint"].value);
	setSessionCookie("state",theForm.elements["state"].value);
	setSessionCookie("scope",theForm.elements["scope"].value);	
	setSessionCookie("autoauthz",theForm.elements["autoauthz"].value);
	setSessionCookie("prompt",theForm.elements["prompt"].value);
	setSessionCookie("nonce",theForm.elements["nonce"].value);
	setSessionCookie("response_type",theForm.elements["response_type"].value);
	// now redirect to the authorization endpoint by simply submitting the form
	theForm.action = theForm.elements["authorize_endpoint"].value;
}

</script>
<title>OAuth 2.0 Implicit Flow</title>
</head>
<body onload="javascript:processAccessToken();">
<%@ include file="header.jsp"%>
<h1>OAuth 2.0 Implicit Flow</h1>
<form name="authform" id="authform" method="GET">
<input type="hidden" name="auto" value="true" />
<table width=800>
<tr><td>Username</td><td><input type="text" name="user_name" /></td></tr>
<tr><td>Client Id</td><td><input type="text" name="client_id" /></td></tr>
<tr><td>Redirect URI</td><td><input type="text" name="redirect_uri" size="60" /></td></tr>
<tr><td>Authorize Endpoint</td><td><input type="text" name="authorize_endpoint" size="60" /></td></tr>
<tr><td>Resource Endpoint</td><td><input type="text" name="resource_endpoint" size="60" /><br>
	<button type="button" onClick="authform.elements['resource_endpoint'].value='<%=urlBase%>'+'/oauth/resource.jsp';">Set Default</button> &nbsp; 
	<button type="button" onClick="authform.elements['resource_endpoint'].value='<%=urlBase%>'+'/oauth/sfresource.jsp';">Set Servlet Filter Default</button>
</td></tr>
<tr><td>State</td><td><input type="text" name="state" size="30" /></td></tr>
<tr><td>Scope</td><td><input type="text" name="scope" size="30" /></td></tr>
<tr><td>autoauthz</td><td><input type="text" name="autoauthz" size="30" /></td></tr>
<tr><td>prompt</td><td><input type="text" name="prompt" size="30" /></td></tr>
<tr><td>Nonce</td><td><input type="text" name="nonce" size="30" /></td></tr>
<tr><td>Response Type</td><td><input type="text" name="response_type" size="30" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="submit" onClick="javascript:processTokenRequest();" style="width:100%">Process Token Request</button></center></td></tr>
</table>
</form>
<script type="text/javascript">
	var theForm = document.getElementById("authform");
	theForm.elements["user_name"].value = username;
	theForm.elements["client_id"].value = clientId;
	theForm.elements["redirect_uri"].value = redirectUri;
	theForm.elements["authorize_endpoint"].value = authorizeEndpoint;
	theForm.elements["resource_endpoint"].value = resourceEndpoint;
	theForm.elements["state"].value = state;
	theForm.elements["scope"].value = scope;
	theForm.elements["autoauthz"].value = autoauthz;	
	theForm.elements["prompt"].value = oidc_prompt;
	theForm.elements["nonce"].value = oidc10_nonce;
	theForm.elements["response_type"].value = response_type;
</script>
</body>
</html>
