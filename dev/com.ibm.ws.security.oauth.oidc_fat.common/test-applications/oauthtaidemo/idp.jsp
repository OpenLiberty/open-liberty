<!--
    Copyright (c) 2020 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import ="java.util.*"%>

<html>
<HEAD>
<link rel="stylesheet" type="text/css" href="one.css">
<SCRIPT LANGUAGE="JavaScript">

//Prepopulating user login information once they return back to the page.
var expDays = 100;
var exp = new Date(); 
exp.setTime(exp.getTime() + (expDays*24*60*60*1000));

function getCookieVal (offset) {  
	var endstr = document.cookie.indexOf (";", offset);  
	if (endstr == -1) { endstr = document.cookie.length; }
	return unescape(document.cookie.substring(offset, endstr));
}

function GetCookie (name) {  
	var arg = name + "=";  
	var alen = arg.length;  
	var clen = document.cookie.length;  
	var i = 0;  
	while (i < clen) {    
		var j = i + alen;    
		if (document.cookie.substring(i, j) == arg) return getCookieVal (j);    
		i = document.cookie.indexOf(" ", i) + 1;    
		if (i == 0) break;   
	}  
	return null;
}

function SetCookie (name, value) {  
	var argv = SetCookie.arguments;  
	var argc = SetCookie.arguments.length;  
	var expires = (argc > 2) ? argv[2] : null;  
	var path = (argc > 3) ? argv[3] : null;  
	var domain = (argc > 4) ? argv[4] : null;  
	var secure = (argc > 5) ? argv[5] : false;  
	document.cookie = name + "=" + escape (value) + 
	((expires == null) ? "" : ("; expires=" + expires.toGMTString())) + 
	((path == null) ? "" : ("; path=" + path)) +  
	((domain == null) ? "" : ("; domain=" + domain)) +    
	((secure == true) ? "; secure" : "");
}

function cookieForms() {  
	var mode = cookieForms.arguments[0];
	for(f=1; f<cookieForms.arguments.length; f++) {
		formName = cookieForms.arguments[f];
		if(mode == 'open') {	
		cookieValue = GetCookie('saved_'+formName);
			if(cookieValue != null) {
				var cookieArray = cookieValue.split('#cf#');
				if(cookieArray.length == document[formName].elements.length) {
					for(i=0; i<document[formName].elements.length; i++) {
						{ document[formName].elements[i].value = (cookieArray[i]) ? cookieArray[i] : ''; }
					}
				}
			}
		}

		if(mode == 'save') {	
			cookieValue = '';
			for(i=0; i<document[formName].elements.length; i++) {
				fieldType = document[formName].elements[i].type;
				{ passValue = document[formName].elements[i].value; }
				cookieValue = cookieValue + passValue + '#cf#';
			}
			cookieValue = cookieValue.substring(0, cookieValue.length-4); // Remove last delimiter
			SetCookie('saved_'+formName, cookieValue, exp);		
		}	
	}
}

</script>
<style>body {
  background: url("4038.jpg") 50% 50% no-repeat;
}
</style>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Redirect To Identity Provider</title>
</head>
<BODY onload="cookieForms('open', 'yourform')" onunload="cookieForms('save', 'yourform')">
<P align="center"><FONT face="Verdana" color="black" size="+2"><B>
</B></FONT></P>
<BR>
<p align="center"><font face="arial" color="navy" size="+3">
<!-- <img border="0" src="kkk.gif" width="350" height="63"> -->
<b>Front End Application</b></font><font color="black">
<br>
</font></p>
<p><br>
<br>
</p>
<form name="yourform" method="post" action="redirectToIdP.jsp">
<center>
<table>
	<tbody>
		<TR align="left">
			<TD bgcolor="transparent" width="185" height="32"><FONT face="arial" size="+1"
				color="black">Single Sign-On URL</FONT></TD>	
			<TD bgcolor="transparent" width="" height="32"><FONT color="black"><INPUT
				type="text"  name="Sso_URL" class="tb7" size="80" value="https://acme:9443/sps/LiangIdp1/saml20/logininitial"></FONT></TD>
			<TD bgcolor="transparent" width="2" height="32">
				<div id="menu"><div class="box">
    					<a href="#nogo"><span class="left"></span><span class="right"></span><span class="lk">
       					 Includes:<br>
       					 <br> 1. https or http <br> 
       					 2. The provider_hostname (The hostname of the provider's point of contact server.)<br>
       					 3. Port_number (The port number of the intersite transfer service endpoint.)<br>
       					 4. sps (This element cannot be changed.)<br>
       					 5. Federation_name (The name you assign to the federation when you create it.)<br>
       					 6. saml20 (The designation of SAML 2.0.)<br>
       					 7. logininitial (This element indicates what type of endpoint is using the port. logininital is used to initiate the single sign-on service.)<br><br>
       					  e.g. https://acme:9443/sps/LiangIdp1/saml20/logininitial  </span></a></div></div></td>
		</TR>
		
		
		<TR align="left">
			<TD bgcolor="transparent" width="185" height="32"><FONT face="arial" size="+1"
				color="black">ACS URL</FONT></TD>
			<TD bgcolor="transparent" width=""><FONT color="black"><INPUT
				type="text" name="PartnerId" class="tb7" size="80" value="https://acme:9443/samlsps/acs"></FONT></TD>
		<TD bgcolor="transparent" width="2" height="32">
				<div id="menu"><div class="box">
    					<a href="#nogo"><span class="left"></span><span class="right"></span><span class="lk">
       					 The ACS URL is the Internet address of the hub's main provisioning server. Known as the provider URL of the target partner.</span></a></div></div></td>
		</TR>

		<TR align="left">
			<TD bgcolor="transparent" width="185" height="15"><FONT face="arial" size="+1"
				color="black">Target URL</FONT></TD>
			<TD bgcolor="transparent" width=""><FONT color="black"><INPUT
				type="text" name="target" class="tb7" size="80" value="https://acme:9443/samldemo/sniff"></FONT></TD>
		<TD bgcolor="transparent" width="2" height="32">
				<div id="menu"><div class="box">
    					<a href="#nogo"><span class="left"></span><span class="right"></span><span class="lk">
       					 The URL of the application that a user can log in to using single sign-on.</span></a></div></div></td>
		</TR>
		
		<TR align="left">
			<TD bgcolor="transparent" width="185" height="15"><FONT face="arial" size="+1"
				color="black">Name Id Format</FONT></TD>
			<TD bgcolor="transparent" width=""><FONT color="black"><INPUT
				type="text" class="tb7" name="NameIdFormat" size="80" value="Email"></FONT></TD>
		<TD bgcolor="transparent" width="2" height="32">
				<div id="menu"><div class="box">
    					<a href="#nogo"><span class="left"></span><span class="right"></span><span class="lk">
       					 The name ID format that is to be used for name identifiers. Valid values are:
						<br><br>Transient (anonymous),Persistent, Encrypted (for encrypted name IDs), Email</span></a></div></div></td>
		</TR>
		
		<TR align="left">
			<TD bgcolor="transparent" width="185" height="15"><FONT face="arial" size="+1"
				color="black">Request Binding</FONT></TD>
			<TD bgcolor="transparent" width=""><FONT color="black"><INPUT
				type="text" class="tb7" name="RequestBinding" size="185" value="HTTPPost" readonly></FONT></TD>
		<TD bgcolor="transparent" width="2" height="32">
				<div id="menu"><div class="box">
    					<a href="#nogo"><span class="left"></span><span class="right"></span><span class="lk">
       					 The protocol binding to be used when sending the request to the partner. Valid values: HTTPPost or HTTPArtifact.</span></a></div></div></td>
		</TR>
		
		<TR align="left">
			<TD bgcolor="transparent" width="185" height="15"><FONT face="arial" size="+1"
				color="black">Allow Create</FONT></TD>
			<TD bgcolor="transparent" width=""><FONT color="black"><INPUT
				type="text" class="tb7" name="AllowCreate" size="185" value="False" readonly></FONT></TD>
		<TD bgcolor="transparent" width="2" height="32">
				<div id="menu"><div class="box">
    					<a href="#nogo"><span class="left"></span><span class="right"></span><span class="lk">
       					 Indicates if new persistent account linkage should be performed on the request. False is the default value. Note: To use this parameter, the NameIdFormat must be set to Persistent.</span></a></div></div></td>
		</TR>
	</tbody>
</table>
</center>

<center>
<TABLE border="0" height="35" width="110">
	<TBODY>
		<TR align="center">
			<TD align="left" valign="middle" height="60" width="185"><FONT
				color="black"><BR>
				
			</FONT>
			<!-- <input type="submit" class="submit" value="" /></TD>-->
			<INPUT type="submit" name="submitButton" class="submit" value="Submit"></TD>
		</TR>
	</TBODY>
</TABLE>
</center>
</form>
</body>
</html>