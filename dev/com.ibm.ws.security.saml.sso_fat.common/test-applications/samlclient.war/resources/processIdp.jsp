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

    String urlIdpRequest =  request.getParameter("urlIdpRequest");
    String urlPartnerId = request.getParameter("urlPartnerId"); 
    String nameIdFormat = request.getParameter("nameIdFormat");  
    String urlTarget = request.getParameter("urlTarget");  
    String testCaseName = request.getParameter("testCaseName");     

    String Target = urlTarget;
    if( testCaseName != null && !testCaseName.isEmpty()){
    	Target = Target + "/" + testCaseName;
    }
    String PartnerId = urlPartnerId;

    String formAction = urlIdpRequest;

%>

<html>
<head>
<link rel="stylesheet" href="template.css" type="text/css">
<meta http-equiv="Pragma" content="no-cache">
<title>Process IDP request</title>
</head>
<body>
<%@ include file="header.jsp"%>
<h1>Process IdP request</h1>
<form name="processidpform" id="processidpform" method="GET" action="<%=formAction%>">
<table width=800>
<tr><td>Target</td><td><input type="text" name="Target" value="<%=Target%>" /></td></tr>
<tr><td>target</td><td><input type="text" name="target" value="<%=Target%>" /></td></tr>
<tr><td>PartnerId</td><td><input type="text" name="PartnerId" value="<%=PartnerId%>" /></td></tr>
<tr><td>providerId</td><td><input type="text" name="providerId" value="<%=PartnerId%>" /></td></tr>
<tr><td>NameIdFormat</td><td><input type="text" name="NameIdFormat" value="<%=nameIdFormat%>" /></td></tr>
<tr><td colspan="2"><center><button type="submit" name="processidpform" style="width:100%">Process IdP request</button></center></td></tr>
</table>  
</form>
<%
System.out.println("processIdp.jsp: Start Test Case:  " + testCaseName + "===============================");
System.out.println("urlIdpRequest:" + urlIdpRequest);
System.out.println("nameIdFormat:" + nameIdFormat);
System.out.println("Target:" + Target);
System.out.println("PartnerId:" + PartnerId);
%>
<script type="text/javascript">
    setTimeout('document.getElementById("processidpform").submit()', 0);
</script>
</body>
</html>
