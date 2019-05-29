<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%-- jsf:pagecode language="java" location="/src/pagecode/aboutusPages/Blah.java" --%><%-- /jsf:pagecode --%><%@page
	language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page import="com.sun.rowset.CachedRowSetImpl" %>
<html>
<head>
<title>blah</title>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<meta name="GENERATOR" content="Rational Application Developer">
</head>

	<body>
Here is the list of Fuel Types:

<table border="1" width="400px">
		<tbody>
		<tr>
			<th>
				FTYPE_NO
			</th>
			<th>
				FTYPE_NAME
			</th>
		</tr>
	<%
	CachedRowSetImpl cacherowset =  (CachedRowSetImpl)request.getAttribute("resultdata");

	while (cacherowset.next()) {
         System.out.println("FTYPE_NO: " + cacherowset.getInt(1) +
            ",  FTYPE_NAME: " + cacherowset.getString(2));
            
	%>
			<tr>
				<td><%=cacherowset.getInt(1) %></td>
				<td><%=cacherowset.getString(2) %></td>
			</tr>

		
<%
	}

 %>
 		</tbody>
	</table>
	</body>

</html>