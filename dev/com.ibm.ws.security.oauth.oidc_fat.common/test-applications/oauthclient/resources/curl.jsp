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

<script language="javascript">
function tokenCurl1() {
	var id = curlform1.elements["client_id"].value;
	var secret = curlform1.elements["client_secret"].value;
	var endpoint = curlform1.elements["token_endpoint"].value;
	var command = 'curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" -d "grant_type=client_credentials&client_id='+id+'&client_secret='+secret+'" '+endpoint;
	curlform1.elements["curl"].value = command;
}

function tokenCurl2() {
	var id = curlform2.elements["client_id"].value;
	var secret = curlform2.elements["client_secret"].value;
	var username = curlform2.elements["username"].value;
	var password = curlform2.elements["password"].value;
	var endpoint = curlform2.elements["token_endpoint"].value;
	var command = 'curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" -d "grant_type=password&client_id='+id+'&client_secret='+secret+'&username='+username+'&password='+password+'" '+endpoint;
	curlform2.elements["curl"].value = command;
}

function resourceCurl() {
	var token = curlform3.elements["access_token"].value;
	var endpoint = curlform3.elements["resource_endpoint"].value;
	var sendtype = "";
	for (var i=0; i<curlform3.elements["sendtype"].length;i++) {
		if (curlform3.elements["sendtype"][i].checked) {
			sendtype = curlform3.elements["sendtype"][i].value;
		}
	} 
	var command = "";
	if (sendtype == "header") {
		command = 'curl -k -H "Authorization: Bearer '+token+'" '+endpoint;
	} else if (sendtype == "query") {
		command = 'curl -k '+endpoint+'?access_token='+token;
	} else {
		command = 'curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" -d "access_token='+token+'" '+endpoint;
	}
	
	curlform3.elements["curl"].value = command;
}

function refreshCurl() {
	var id = curlform4.elements["client_id"].value;
	var secret = curlform4.elements["client_secret"].value;
	var refresh_token = curlform4.elements["refresh_token"].value;
	var endpoint = curlform4.elements["token_endpoint"].value;
	var command = 'curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" -d "grant_type=refresh_token&client_id='+id+'&client_secret='+secret+'&refresh_token='+refresh_token+'" '+endpoint;
	curlform4.elements["curl"].value = command;
}
</script>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>Curl Test Client</h1>
'curl' is an open-source command line tool for transferring data with URL syntax.<br>
<br>
Since the OAuth 2.0 network commands are relatively
simple, it is easy to use a tool like curl to drive OAuth flows.<br>
<br>
	<table width=800><tr><td>
	<b>Download curl if required</b><br>
	For most unix platforms curl will be standard. For windows it is available as part of cygwin, and for more information see <a href="http://curl.haxx.se/download.html">http://curl.haxx.se/download.html</a><br>
	</td></tr></table>
<br>
The 3-legged OAuth flows require interactive authorization with a redirect, so it's easiest to retrieve an
authorization code before using curl.<br>
<br> 
For a simple example we can run curl using a flow that does
not require the authorization endpoint, such as Client Credentials flow or Resource Owner password credentials flow.<br>
This example assumes you are running the example OAuth server environment at https://localhost:9443, however replace with whatever you are really using.<br>
<br>
	<table width=800><tr><td>
	<b>Client Credentials Token Request</b><br>
	curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" -d "grant_type=client_credentials&client_id=<b><font color="BROWN">YOUR_CLIENT_ID</font></b>&client_secret=<b><font color="BROWN">YOUR_CLIENT_SECRET</font></b>" https://localhost:9443/oauth/token.jsp</font></b>
	</td></tr></table>
<br>
Running this command successfully results in a JSON response similar to:
	<pre>
	{"access_token":"DHZFehg6lEPP23aC0nQ9PZd0B5nwTMvtQcD3plEN","token_type":"bearer","expires_in":3600}</pre>
The access token can be used with curl to request a resource.<br>
There are three different methods available to transmit an access token - Authorization header, query string and post body.<br>
These examples assume your resource server is https://localhost:9443, however replace with whatever you are really using.<br>
<br>
	<table width=800><tr><td>
	<b>Resource Request Using Authorization Header</b><br>
	curl -k -H "Authorization: Bearer <b><font color="BROWN">YOUR_TOKEN</font></b>" https://localhost:9443/oauth/sfresource.jsp<br>
	<b>Resource Request Using Query String</b><br>
	curl -k https://localhost:9443/oauth/sfresource.jsp?access_token=<b><font color="BROWN">YOUR_TOKEN</font></b><br>
	<b>Resource Request Using POST body</b><br>
	curl -k -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" -d "access_token=<b><font color="BROWN">YOUR_TOKEN</font></b>" https://localhost:9443/oauth/sfresource.jsp
	</td></tr></table>
<br>
Use these forms to generate sample curl commands:<br>


<br>
<form name="curlform1" method="GET" onsubmit="">
	<table width=800>
	<tr><td colspan="2"><b>Access Token Request for Client Credentials Flow</b></td></tr>
	<tr><td>Client Id</td><td><input type="text" name="client_id" value="key" /></td></tr>
	<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="secret" /></td></tr>
	<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="https://localhost:9443/oauth/token.jsp" size="60" /></td></tr>
	<tr><td colspan="2"><button type="button" name="generate" onClick="tokenCurl1();">Generate Curl Command</button></td></tr>
	<tr><td>Command</td><td><textarea readonly name="curl" rows="5" cols="60" ></textarea></td></tr>
	</table>
</form>

<br>
<form name="curlform2" method="GET" onsubmit="">
	<table width=800>
	<tr><td colspan="2"><b>Access Token Request for Resource Owner Password Credentials Flow</b></td></tr>
	<tr><td>Client Id</td><td><input type="text" name="client_id" value="key" /></td></tr>
	<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="secret" /></td></tr>
	<tr><td>Resource Owner Username</td><td><input type="text" name="username" value="user1" /></td></tr>
	<tr><td>Resource Owner Password</td><td><input type="text" name="password" value="pass1" /></td></tr>
	<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="https://localhost:9443/oauth/token.jsp" size="60" /></td></tr>
	<tr><td colspan="2"><button type="button" name="generate" onClick="tokenCurl2();">Generate Curl Command</button></td></tr>
	<tr><td>Command</td><td><textarea readonly name="curl" rows="5" cols="60" ></textarea></td></tr>
	</table>
</form>

<br>     
<form name="curlform3" method="GET" onsubmit="">
	<table width=800>
	<tr><td colspan="2"><b>Resource Request</b></td></tr>
	<tr><td>Access Token</td><td><input type="text" name="access_token" value="" size="60" /></td></tr>
	<tr><td>Resource Endpoint</td><td><input type="text" name="resource_endpoint" value="https://localhost:9443/oauth/sfresource.jsp" size="60" /></td></tr>
	<tr><td>Token Transmission</td><td><input type="radio" name="sendtype" value="header" checked/>Header<br><input type="radio" name="sendtype" value="query"/>Query String<br><input type="radio" name="sendtype" value="body"/>POST Body</td></tr>
	<tr><td colspan="2"><button type="button" name="generate" onClick="resourceCurl();">Generate Curl Command</button></td></tr>
	<tr><td>Command</td><td><textarea readonly name="curl" rows="5" cols="60" ></textarea></td></tr>
	</table>
</form>

<br>     
<form name="curlform4" method="GET" onsubmit="">
	<table width=800>
	<tr><td colspan="2"><b>Refresh Token Exchange Request</b></td></tr>
	<tr><td>Client Id</td><td><input type="text" name="client_id" value="key" /></td></tr>
	<tr><td>Client Secret</td><td><input type="text" name="client_secret" value="secret" /></td></tr>
	<tr><td>Refresh Token</td><td><input type="text" name="refresh_token" value="" size="60" /></td></tr>
	<tr><td>Token Endpoint</td><td><input type="text" name="token_endpoint" value="https://localhost:9443/oauth/token.jsp" size="60" /></td></tr>
	<tr><td colspan="2"><button type="button" name="generate" onClick="refreshCurl();">Generate Curl Command</button></td></tr>
	<tr><td>Command</td><td><textarea readonly name="curl" rows="5" cols="60" ></textarea></td></tr>
	</table>
</form>

</body>
