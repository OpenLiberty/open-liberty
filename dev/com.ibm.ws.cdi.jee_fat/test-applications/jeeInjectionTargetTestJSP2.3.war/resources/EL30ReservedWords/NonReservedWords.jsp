<%@ page import="com.ibm.ws.cdi.vistest.masked.beans.EL30ReserverdWordsTestBean"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test the EL 3.0 Non-Reserved Words</title>
</head>
<body>

	<%-- Create an instance of the ReservedWordsTestBean --%>
    <%!
    	EL30ReserverdWordsTestBean test = new EL30ReserverdWordsTestBean();
    %>
    <%  
    	test.setCat("Testing \"cat\" non-reserved word. Test Successful");
    	test.setT("Testing \"T\" non-reserved word. Test Successful"); 
    	request.setAttribute("test", test);
    %>

	<%-- Test the EL 3.0 cat and T Non-Reserved Words --%>
	<%
		try {
			if(request.getParameter("testNonReservedWord").equals("cat")) { %>
				${test.cat}
	<% 		}
			else if(request.getParameter("testNonReservedWord").equals("T")) { %>
				${test.t}
	<%		}
			else {
				out.print("Invalid parameter");
	   		} 
		} catch(Exception e) {
			out.print("Exception caught: " + e.getMessage() + "<br/>");
        	out.print("Test Failed. An exception was thrown: " + e.toString() + "<br/>");
		}
	%>
	
</body>
</html>