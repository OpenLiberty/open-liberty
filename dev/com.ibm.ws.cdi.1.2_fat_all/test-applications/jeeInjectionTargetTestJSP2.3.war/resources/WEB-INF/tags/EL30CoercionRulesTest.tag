<%@taglib prefix="test" tagdir="/WEB-INF/tags" %>
<%@attribute name="number" required="true" type="java.lang.Double" %>
<%@tag body-content="empty" %>

Testing Coercion of a Value X to Type Y.
<br/>
Test if X is null and Y is not a primitive type and also not a String, return null (Expected:true): ${number == null}


