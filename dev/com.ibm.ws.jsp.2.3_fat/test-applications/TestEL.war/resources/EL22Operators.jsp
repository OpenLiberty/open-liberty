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
<title>JSP to test the existing EL 2.2 Operators</title>
</head>
<body>
        <b>Test ${testNum=1}:</b> EL 2.2 Multiplication Operator (Expected:16): ${8*2}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Addition Operator (+) (Expected:5): ${2+3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Subtraction Operator (-) (Expected:1): ${5-4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Division Operator (/) (Expected:8.0): ${16/2}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Division Operator (div) (Expected:8.0): ${16 div 2}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Remainder Operator (%) (Expected:1): ${19%2}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Remainder Operator (mod) (Expected:1): ${19 mod 2}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (==) (Expected: true): ${3 == 3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (eq) (Expected: false): ${3 eq 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (!=) (Expected: true): ${3 != 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (ne) (Expected: false): ${3 ne 3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (<) (Expected: true): ${3 < 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (lt) (Expected: false): ${5 lt 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (>) (Expected: false): ${3 > 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (gt) (Expected: true): ${5 gt 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (<=) (Expected: true): ${3 <= 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (le) (Expected: false): ${5 le 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (le) (Expected: true): ${3 le 3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (>=) (Expected: true): ${5 >= 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (ge) (Expected: false): ${3 ge 4}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Relational Operator (ge) (Expected: true): ${3 ge 3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (&&) (Expected: false): ${true && false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (&&) (Expected: true): ${true && true}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (and) (Expected: false): ${false and false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (||) (Expected: true): ${true || false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (||) (Expected: false): ${false || false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (or) (Expected: true): ${true or false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (!) (Expected: false): ${!true}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (!) (Expected: true): ${!false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Logical Operator (not) (Expected: true): ${not false}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Empty Operator (empty) (Expected: true): ${empty z}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Empty Operator (empty) (Expected: false): ${x=5; empty x}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Conditional Operator (A?B:C) (Expected: 2): ${1==1?2:3}
    <p/>
        <b>Test ${testNum=testNum+1}:</b> EL 2.2 Conditional Operator (A?B:C) (Expected: 3): ${1==2?2:3}
    <p/>
</body>
</html>
