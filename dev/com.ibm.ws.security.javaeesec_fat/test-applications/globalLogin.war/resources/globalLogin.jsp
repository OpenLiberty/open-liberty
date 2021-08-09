<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Global Form Login Page</title>
</head>
<body>
<h2>Global Form Login Page</h2>
<form action="j_security_check" method="POST">
UserName: <input type="text" name="j_username">
Password: <input type="password" name="j_password">
<input type="submit" name="action" value="Login">
</form>
</body>
</html>