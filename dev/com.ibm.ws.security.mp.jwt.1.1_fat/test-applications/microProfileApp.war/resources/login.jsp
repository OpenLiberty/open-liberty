<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<META name="GENERATOR" content="IBM Software Development Platform">
<TITLE>login.jsp</TITLE>
</HEAD>
<BODY>
<h2>Form Login Page</h2>
<FORM action="j_security_check" method="POST">
UserName: <INPUT type="text" name="j_username">
Password: <INPUT type="password" name="j_password">
OpenID: <INPUT type="text" name="openid_identifier">
<br>
Description: <INPUT type="text" name="j_description">
<INPUT type="submit" name="action" value="Login">
</BODY>
</FORM>
</HTML>