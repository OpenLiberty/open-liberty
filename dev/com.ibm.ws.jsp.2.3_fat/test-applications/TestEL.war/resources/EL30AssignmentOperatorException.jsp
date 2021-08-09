<!--
    Copyright (c) 2015 IBM Corporation and others.
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
<title>JSP EL 3.0 assignment operator exception</title>
</head>
<body>
		<%System.out.println("testEL30AssignmentOperatorException , javax.el.PropertyNotWritableException is expected in logs");%>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment Operator (Expected:exception): ${null=(x->x+1)(2)}</b>
        
</body>
</html>
