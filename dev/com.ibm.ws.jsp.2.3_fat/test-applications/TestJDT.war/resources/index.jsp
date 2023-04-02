<!--
    Copyright (c) 2021 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<title>TestJDT</title>
</head>
<body>
<%
class InnerClass {
    public int counter = 0;
	public int r1 () {
    	return 1;	
    }
    
    public int r2 () {
    	return 2;
    }
    public void resetCounter() {
    	counter = 0;
    }
}

List<InnerClass> list = new ArrayList<>();

InnerClass iC = new InnerClass();

for(InnerClass elements : list) {
    // do sth
    iC.counter++;
}
%>
<!-- After JDT update to 3.22, the above construct fails compile with a ClassCastException.
If this compiles OK, then fix for issue 19197 works.  -->
<p>Test passed.</p>
</body> 
</html> 
