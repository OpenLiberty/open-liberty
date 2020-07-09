<%@ page import="com.ibm.ws.cdi.vistest.masked.beans.EL30InvocationMethodExpressionTestBean"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test the EL 3.0 Invocation of Method Expressions</title>
</head>
<body>

	<%! EL30InvocationMethodExpressionTestBean parent = new EL30InvocationMethodExpressionTestBean(); %>
	
	<%-- Set the parent and child objects using JSP Scriptless --%>
	<% 
		parent.setParentName("John Smith Sr.");
		parent.getChild().setChildName("John Smith Jr.");
		
		request.setAttribute("parent", parent);
	%>
	
	<%-- Lets first use Value Expressions to perform invocations --%>
	Get Parent Name Using Value Expression (Expected: "John Smith Sr."): ${parent.parentName}
	<br/>
	Get Child Name Using Value Expression (Expected: "John Smith Jr."): ${parent.child.childName}
	<br/>
	Get Object Representation Using Value Expression: ${parent}
	<br/>
	
	<%-- Now we use Method Expressions to perform invocations --%>
	
	<%-- Set the parent object method using Method Expression --%>
	${parent.setParentName('Steven Johnson Sr.')}
	
	<%-- Set the child object method using Method Expression --%>
	${parent.child.setChildName('Steven Johnson Jr.')}
	
	Get Parent Name Using Method Expression (Expected: "Steven Johnson Sr."): ${parent.getParentName()}
	<br/>
	Get Child Name Using Method Expression (Expected: "Steven Johnson Jr."): ${parent.child.getChildName()}
	<br/>
	Get Object Representation Using Method Expression: ${parent.toString()}
	<br>

</body>
</html>