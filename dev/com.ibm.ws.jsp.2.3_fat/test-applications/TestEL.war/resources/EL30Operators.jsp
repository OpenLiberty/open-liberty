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
<title>JSP to test the new EL 3.0 Operators</title>
</head>
<body>
    <h2>
        In order for all of the expected results to be correct the following query parameters should be used: ?testString1=1&testString2=2
    </h2>
    <p/>
        <b>Test ${testNum=1}:</b> EL 3.0 String Concatenation Operator (+=) with literals (Expected: xy): ${"x" += "y"}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator (+=) with variables (Expected: 12): ${param.testString1 += param.testString2}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator with literals and multiple concatenations (Expected: xyz): ${"x" += "y" += "z"}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator with literals and single quotes  (Expected: xyz): ${'x' += 'y' += 'z'}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator with literals and mixed quotes  (Expected: xyz): ${"x" += 'y' += "z"}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: "x"yz): ${"\"x\"" += 'y' += "z"}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: 'x'yz): ${"\'x\'" += 'y' += "z"}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment Operator (=) (Expected:3): ${x=3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment Operator (=) (Expected:8): ${y=x+5}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment Operator (Expected:3): ${x=(x->x+1)(2)}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 3.0 Semi-colon Operator (Expected:8): ${x = 5; y = 3; z = x + y}
    <p/>
    <p/>)
</body>
</html>
