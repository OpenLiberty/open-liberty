<html>
<head>
<title>Guest Book Servlet</title>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
</head>
<body>
    <h1>Guest Book - JPA 2.1 Application</h2>
    <h3>Guest Signin:</h2>
    <form action="GuestBookServlet" method="POST">
        First name:<br>
        <input name="firstName" type="text" /><br><br>
        Last name:<br>
        <input name="lastName" type="text" /><br><br>
        <input name="send" type="submit" value="Enter" />
    </form>
    <h3>Guests Signed In:</h2>
    <table border="1">
        <tr>
            <td>First Name</td>
            <td>Last Name</td>
            <td>Sign In Date</td>
        </tr>
        <c:forEach items="${guests}" var="guest">
            <tr>
                <td>${guest.firstName}</td>
                <td>${guest.lastName}</td>
                <td>${guest.localDateTime}</td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>