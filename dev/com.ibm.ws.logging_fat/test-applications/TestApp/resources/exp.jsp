<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%! 
public static class ExceptionOne extends RuntimeException {
	public ExceptionOne() {
		super();
	}
	public ExceptionOne(String message) {
		super(message);
	}
}
public static class ExceptionTwo extends RuntimeException {
	public ExceptionTwo() {
		super();
	}
	public ExceptionTwo(String message) {
		super(message);
	}
}
public static class ExceptionThree extends RuntimeException {
	public ExceptionThree() {
		super();
	}
	public ExceptionThree(String message) {
		super(message);
	}
}
public static class ExceptionFour extends RuntimeException {
	public ExceptionFour() {
		super();
	}
	public ExceptionFour(String message) {
		super(message);
	}
}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Throw-a-exception</title>
</head>
<body>
</body>
</html>
<%
String message = request.getParameter("message");
if(message==null) {
	message = "A exception.";
}

boolean exceptionOne = Boolean.parseBoolean(request.getParameter("ExceptionOne"));
if(exceptionOne) {
	throw new ExceptionOne(message);
}

boolean exceptionTwo = Boolean.parseBoolean(request.getParameter("ExceptionTwo"));
if(exceptionTwo) {
	throw new ExceptionTwo(message);
}

boolean exceptionThree =  Boolean.parseBoolean(request.getParameter("ExceptionThree"));
if(exceptionThree) {
	throw new ExceptionThree(message);
}

boolean exceptionFour = Boolean.parseBoolean(request.getParameter("ExceptionFour"));
if(exceptionFour) {
	throw new ExceptionFour(message);
}

// ELSE
if(message!=null) {
    throw new RuntimeException(message);
}%>
