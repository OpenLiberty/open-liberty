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
<%@ page import ="java.lang.*" %> 
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>EL30Lambda</title>
</head>
<body>
	<p>			
		<%System.out.println("Test1_LambdaParam");%>
		<b>LambdaParam_EL3.0_Test ${testNum=1}:</b> --> ${(x->x+1)(8)}		
	</p>	
	<p>			
		<%System.out.println("Test2_RejectExtraLambdaParam");%>
		<b>RejectExtraLambdaParam_EL3.0_Test ${testNum=2}:</b> --> ${((firstStr)-> (firstStr.length()))("First","Blah")}
	</p>
	<p>			
		<%System.out.println("Test3_MultipleLambdaParams");%>
		<b>MultipleLambdaParams_EL3.0_Test ${testNum=3}:</b> --> ${((x,y)->x+y)(8,9)}
	</p>			
	<p>		
	<% try { 
		System.out.println("Test4_CatchExeptionOnLessParam");%>		
		${((x,y)->x+y)(8)}		
	<%}catch(Exception e){
			// javax.el.ELException: Only [1] arguments were provided for a lambda expression that requires at least [2]
			String blah = e.getMessage().toString();  %>			
			<b>CatchExeptionOnLessParam_EL3.0_Test ${testNum=4}:</b> --> Pass another argument. <%=blah %>
	<%	} %>
	</p>	
	<p>		
		<% System.out.println("Test5_AssignedLambdaExp");%>
		<b>AssignedLambdaExp_EL3.0_Test ${testNum=5}:</b> --> ${incr =(x->x+1)(8)}
	</p>	
	<p>	
		<%System.out.println("Test6_NoParam");%>
		<b>NoParam_EL3.0_Test ${testNum=6}:</b> --> ${(()->64)(8)}		
	</p>
	<p>	
		<%System.out.println("Test7_OptionalParenthesis");%>
		<b>OptionalParenthesis_EL3.0_Test ${testNum=7}:</b> --> ${(x->64)(8)}		
	</p>
	<p>
		<%System.out.println("Test8_PrintFromBody");%>
		<b>PrintFromBody_EL3.0_Test ${testNum=8}:</b> --> ${(()->(System.out.println("Hello World")))}		
	</p>
	<p>	
		<%System.out.println("Test9_ParameterCocerceToString");%>
		<b>ParameterCocerceToString_EL3.0_Test ${testNum=9}:</b> --> ${( (firstStr, secondStr)-> (Integer.compare(firstStr.length(),secondStr.length())) )("Do","Blah")}
	</p>	
	<p>	
		<%System.out.println("Test10_ParameterCocerceToInt");%>
		<b>ParameterCocerceToInt_EL3.0_Test ${testNum=10}:</b> --> ${((firstStr, secondStr)-> (Integer.compare(firstInt,secondInt)))(5,6)}
	</p>
	<p>
		<%System.out.println("Test11_InvokeFunctionIndirect");%>	
		<b>InvokeFunctionIndirect_EL3.0_Test ${testNum=11}:</b> --> ${incr = x->x+1; incr(10)}
	</p>
	<p>
		<%System.out.println("Test12_InvokeFunctionIndirect2");%>	
		<b>InvokeFunctionIndirect2_EL3.0_Test ${testNum=12}:</b> --> ${ fact = n -> n==0? 1: n*fact(n-1); fact(5)}
	</p>
	<p>	
		<%System.out.println("Test13_PassedAsArgumentToMethod");%>	
		<jsp:useBean id="employee" class="com.ibm.ws.jsp23.fat.testel.beans.Employee" scope="request">
			<jsp:setProperty  name="employee" property="firstname" value="Charlie" />
			<jsp:setProperty  name="employee" property="lastname" value="Brown" />
		</jsp:useBean>	
		<b>PassedAsArgumentToMethod_EL3.0_Test ${testNum=13}:</b> -->  ${employee.firstname} ${employee.sanitizeNames(e->e.firstname == 'Charlie')};
	</p>
	<p>	
		<%System.out.println("Test14_NestedFunction1");%>
		<b>Nested1_EL3.0_Test ${testNum=14}:</b> --> ${sum = x -> (y -> y+y)(x) + x ; sum(4) }
	</p>
	<p>	
		<%System.out.println("Test15_NestedFunction2");%>
		<b>Nested2_EL3.0_Test ${testNum=15}:</b> --> ${parseMe = x -> (y -> (Integer.parseInt(y)))(x) + x ; parseMe("1234") }
	</p>
	
</body>
</html>
