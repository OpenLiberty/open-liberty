<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.logging.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>print all JUL log levels</title>
</head>
<body>
<%
System.out.println("In JUL.jsp");
String messageKey = request.getParameter("messageKey");
if(messageKey==null) {
	messageKey="new log message @ " + System.currentTimeMillis();
}

String componentName = "collector.manager_fat.JUL_jsp;"; //this.getClass().getName();
Logger logger = Logger.getLogger(componentName,"JulMessages");
String sourceClass = this.getClass().getName();
// System.out.println("componentName =" + componentName);

// #1
System.out.println("JUL_jsp: sysout for " + messageKey);
logger.config("JUL_jsp: config log for " + messageKey);
logger.entering(sourceClass, "JUL_jsp_entering_method_for_"+messageKey);
logger.exiting(sourceClass, "JUL_jsp_exiting_method_for_"+messageKey);
logger.fine("JUL_jsp: fine log for " + messageKey);
logger.finer("JUL_jsp: finer log for " + messageKey);
logger.finest("JUL_jsp: finest log for " + messageKey);
logger.info("JUL_jsp: info log for "+messageKey);
logger.severe("JUL_jsp: severe log for "+messageKey);
logger.warning("JUL_jsp: warning log for "+messageKey);
try {
	throw new RuntimeException("Exception for "+messageKey);
}
catch(Exception ex) {
	logger.throwing(sourceClass, "JUL_jsp_throwing_method_for_"+messageKey, ex);
}

// #2
logger.fine("JUL_jsp: fine log for [2]" + messageKey);
logger.finer("JUL_jsp: finer log for [2]" + messageKey);
logger.finest("JUL_jsp: finest log for [2]" + messageKey);
logger.info("JUL_jsp: info log for [2]"+messageKey);
logger.severe("JUL_jsp: severe log for [2]"+messageKey);
logger.warning("JUL_jsp: warning log for [2]"+messageKey);

%>
<h1>All Logs Printed</h1>
</body>
</html>