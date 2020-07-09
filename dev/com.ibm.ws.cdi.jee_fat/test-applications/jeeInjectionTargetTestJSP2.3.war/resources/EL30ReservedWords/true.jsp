<%@ page import="com.ibm.ws.cdi.vistest.masked.beans.EL30ReserverdWordsTestBean"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>JSP to test the EL 3.0 Reserved Words</title>
</head>
<body>

	<%-- Create an instance of the ReservedWordsTestBean --%>
    <%!
    	EL30ReserverdWordsTestBean test = new EL30ReserverdWordsTestBean();
    %>
    <%   
    	request.setAttribute("test", test);
    %>
	
	<%-- Test the EL 3.0 "true" Reserved Word --%>
	${test.true}
	
</body>
</html>