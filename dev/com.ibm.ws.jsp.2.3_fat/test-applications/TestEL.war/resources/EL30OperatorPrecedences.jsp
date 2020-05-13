<!--
    Copyright (c) 2015 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<%@ page import="com.ibm.ws.jsp23.fat.testel.beans.EL30InvocationMethodExpressionTestBean"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test the EL 3.0 Operator Precedence</title>
</head>
<body>
	
	<%-- Create an instance of the InvocationMethodExpressionTest bean to be used with [] and . operators --%>
	<% 
		EL30InvocationMethodExpressionTestBean parent = new EL30InvocationMethodExpressionTestBean();
		request.setAttribute("parent", parent);
	%>
	
	${parent.setParentName('John Smith Sr.')}  
	
	<p>
		<b>Test ${testNum=1}:</b> EL 3.0 [] and . operators left-to-right (Expected:true): ${parent.parentName == parent['parentName']} 
	</p> 
	
	<%-- EL 3.0 [] operator with Parenthesis operator --%>
	${parent.child.setChildName(parent['parentName'])}
	
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 [] and . operators left-to-right (Expected:true): ${parent.child.childName == parent.child['childName']}  
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Parenthesis Operator with - (unary) (Expected:-14): ${-8-(4+2)}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Parenthesis Operator with - (unary) (Expected:-10): ${(-8-4)+2}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 not ! empty operators left-to-right (Expected:true): ${z=null; not false && empty z}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Parenthesis Operator with not ! empty operators (Expected:true): ${x=2; (empty x && not false) || !false}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Parenthesis Operator with not ! empty operators (Expected:false): ${empty x && (not false || !false)}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 * / div % mod operators left-to-right (Expected:1.0): ${4*8/8%3}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Parenthesis Operator with * / div % mod operators (Expected:16.0): ${4*8 div (8 mod 3)}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 + - operators left-to-right (Expected:5): ${2+8-5}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 + - * / div operators (Expected:31.0): ${2+4*8-24/8}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Parenthesis Operator with + - * / div operators (Expected:45.0): ${(2+4)*8-24/8}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 String Concatenation Operator (+=) and + operator (Expected:3abc): ${1 + 2 += "abc"}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 < > <= >= lt gt le ge relational operators left-to-right (Expected:true): ${1 < 3 && 3 > 2 && 3 <= 3 && 2 >= 1 && 1 lt 3 && 3 gt 2 && 3 le 3 && 2 ge 1}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 < > relational operators with + - operators (Expected:false): ${4 + 6 > 9 && 8 - 3 < 5}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 == != eq ne relational operators left-to-right (Expected:true): ${3 == 3 && 3 != 4 && 5 eq 5 && 5 ne 6}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 == and <= relational operators (Expected:true): ${true == 1 <= 1}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 != and > relational operators (Expected:false): ${false != 1 > 1}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 && and || logical operators (Expected:true): ${true || true && false}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 and or logical operators (Expected:true): ${true or true and false}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 ? and : conditional operators (Expected:2): ${1==1&&true?2:3}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 ? and : conditional operators (Expected:3): ${1==1&&false?2:3}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 -> (lambda) operator (Expected:60): ${((a, b) -> a>b?50:60)(2, 5)}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with concatenation operator (+=) (Expected:13): ${a = 1; b = 3; w = a += b}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:5): ${v = (x->x+1)(3); v = v + 1}
	</p>
	<p>
		<b>Test ${testNum=testNum+1}:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:11): ${(x->(a=x))(10); a = a + 1}
	</p>
		
</body>
</html>
