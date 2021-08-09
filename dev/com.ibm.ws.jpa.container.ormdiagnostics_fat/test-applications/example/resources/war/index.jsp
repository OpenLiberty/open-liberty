<html>
<head>
<title>Example Servlet</title>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
</head>
<body>
    <h1>Entities - JPA 2.1 Application</h2>
    <h3>Add Entity:</h2>
    <form action="ExampleServlet" method="POST">
        String 1:<br>
        <input name="str1" type="text" /><br><br>
        String 2:<br>
        <input name="str2" type="text" /><br><br>
        String 3:<br>
        <input name="str3" type="text" /><br><br>
        <input name="send" type="submit" value="Enter" />
    </form>
    <h3>Current Entities:</h2>
    <table border="1">
        <tr>
            <td>String 1</td>
            <td>String 2</td>
            <td>String 3</td>
        </tr>
        <c:forEach items="${entities}" var="entity">
            <tr>
                <td>${entity.str1}</td>
                <td>${entity.str2}</td>
                <td>${entity.str3}</td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>