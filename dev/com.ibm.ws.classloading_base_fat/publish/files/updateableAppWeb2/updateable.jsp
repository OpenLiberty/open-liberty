<html>
<title>updateable.jsp</title>
<body>
	<%
		String greeting = request.getParameter("greeting");
		String userName = "Steve";
		System.out.println("executing updateable.jsp - printing " + greeting + " " + userName);
	%>
	<div><%=greeting%> <%=userName%></div>
</body>
</html>