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



<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Redirect To Identity Provider</title>
</head>
<body>
<P align="center"><FONT face="Verdana" color="black" size="+2"><B>
</B></FONT></P>
<BR>
<p align="center"><font face="Verdana" color="black" size="+2">
<b>Login To IBM TFIM Single Sign-On Service</b></font><font color="black"><br>
</font></p>
<p><br>
<br>
</p>
<form method="post" action="redirectToIdP.jsp">
<center>
<table>
	<tbody>
		<TR align="left">
			<TD bgcolor="white" width="250"><FONT face="Verdana" size="+1"
				color="black">Single Sign-On URL</FONT>
			</TD>
			<TD bgcolor="white" width="250">
			   	<FONT color="black"><INPUT
				type="text" name="Sso_URL" size="80" value="https://liangch-linux.austin.ibm.com:9443/sps/LiangIdp1/saml20/logininitial">
				</FONT>
			</TD>
		</TR>
		
		<TR align="left">
			<TD bgcolor="white" width="250"><FONT face="Verdana" size="+1"
				color="black">ACS URL</FONT></TD>
			<TD bgcolor="white" width="250"><FONT color="black"><INPUT
				type="text" name="PartnerId" size="80" value="https://liang2007.austin.ibm.com:9443/samlsps/acs"></FONT></TD>
		</TR>

		<TR align="left">
			<TD bgcolor="white" width="250"><FONT face="Verdana" size="+1"
				color="black">Target URL</FONT></TD>
			<TD bgcolor="white" width="250"><FONT color="black"><INPUT
				type="text" name="target" size="80" value="https://liang2007.austin.ibm.com:9443/samldemo/sniff"></FONT></TD>
		</TR>
		
		<TR align="left">
			<TD bgcolor="white" width="250"><FONT face="Verdana" size="+1"
				color="black">Name Id Format</FONT></TD>
			<TD bgcolor="white" width="250"><FONT color="black"><INPUT
				type="text" name="NameIdFormat" size="80" value="email"></FONT></TD>
		</TR>
		
		<TR align="left">
			<TD bgcolor="white" width="250"><FONT face="Verdana" size="+1"
				color="black">Request Binding</FONT></TD>
			<TD bgcolor="white" width="250"><FONT color="black"><INPUT
				type="text" name="RequestBinding" size="80" value="HTTPPost"></FONT></TD>
		</TR>
		
		<TR align="left">
			<TD bgcolor="white" width="250"><FONT face="Verdana" size="+1"
				color="black">Allow Create</FONT></TD>
			<TD bgcolor="white" width="250"><FONT color="black"><INPUT
				type="text" name="AllowCreate" size="80" value="false"></FONT>
			</TD>
		</TR>
		

		
	</tbody>
</table>
</center>
<CENTER>
<TABLE border="0" height="35" width="110">
	<TBODY>
		<TR align="center">
			<TD align="left" valign="middle" height="60" width="250"><FONT
				color="black"><BR>
			</FONT><INPUT type="submit" name="submitButton" value="Submit"></TD>
		</TR>
	</TBODY>
</TABLE>
</CENTER>
<p align="left"></p>
</form>

</body>
</html>